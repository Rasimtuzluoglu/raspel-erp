package com.raspel.erp.config;

import com.raspel.erp.config.security.JwtUtil;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.SohbetOdaUyeRepository;
import com.raspel.erp.service.sistem.AktifOturumService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final Pattern SIRKET_TOPIC_PATTERN =
            Pattern.compile("^/topic/(bildirimler)/(\\d+)$");

    /** Genel sohbet kanalı ve silme olayı (Faz 3.1: sil de doğrulanır). */
    private static final Pattern SOHBET_GENEL_PATTERN =
            Pattern.compile("^/topic/sohbet/(\\d+)$");
    private static final Pattern SOHBET_GENEL_SIL_PATTERN =
            Pattern.compile("^/topic/sohbet/(\\d+)/sil$");

    private static final Pattern ODA_TOPIC_PATTERN =
            Pattern.compile("^/topic/sohbet/oda/(\\d+)/(\\d+)$");

    private static final Pattern ODA_SIL_TOPIC_PATTERN =
            Pattern.compile("^/topic/sohbet/oda/(\\d+)/(\\d+)/sil$");

    private static final Pattern ODA_YAZIYOR_TOPIC_PATTERN =
            Pattern.compile("^/topic/sohbet/oda/(\\d+)/(\\d+)/yaziyor$");

    private final JwtUtil jwtUtil;
    private final KullaniciRepository kullaniciRepository;
    private final AktifOturumService aktifOturumService;
    private final SohbetOdaUyeRepository sohbetOdaUyeRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionAttrs = (Map<String, Object>) accessor.getSessionAttributes();
            String token = sessionAttrs != null ? (String) sessionAttrs.get("token") : null;
            if (token == null || !jwtUtil.validateToken(token)) {
                throw new MessageDeliveryException("Kimlik doğrulama gerekli");
            }
            // İptal edilmiş oturumlar WebSocket'te de geçersizdir.
            if (aktifOturumService.iptalEdilmis(jwtUtil.getJtiFromToken(token))) {
                throw new MessageDeliveryException("Oturum sonlandırılmış");
            }
            String username = jwtUtil.getUsernameFromToken(token);
            Kullanici k = kullaniciRepository.findByUsername(username).orElse(null);
            if (k == null || !Boolean.TRUE.equals(k.getActive())) {
                throw new MessageDeliveryException("Kullanıcı aktif değil");
            }
            // "En son giris kazanir": gecersiz (eski) oturumla WS baglantisi kurulamaz.
            if (!aktifOturumService.aktifOturumMu(k.getId(), jwtUtil.getJtiFromToken(token))) {
                throw new MessageDeliveryException("Bu oturum başka bir cihazda sona erdi");
            }
            // Parola değişiminde tokenVersion artar; eski token ile WS bağlantısı kurulamaz.
            Long tokenVersion = jwtUtil.getTokenVersionFromToken(token);
            long dbVersion = k.getTokenVersion() != null ? k.getTokenVersion() : 0L;
            if (tokenVersion != null && tokenVersion.longValue() != dbVersion) {
                throw new MessageDeliveryException("Oturum geçersiz");
            }
            Long sirketId = jwtUtil.getSirketIdFromToken(token);
            // Token'daki şirket, kullanıcının şirketiyle uyuşmalıdır (ADMIN firma değiştirebilir).
            if (sirketId == null || (k.getSirketId() != null && !"ADMIN".equals(k.getRole())
                    && !sirketId.equals(k.getSirketId()))) {
                throw new MessageDeliveryException("Şirket bilgisi geçersiz");
            }
            accessor.setUser(() -> username);
            if (sessionAttrs != null) {
                sessionAttrs.put("sirketId", sirketId);
                // Faz 3.1: oda üyeliği doğrulaması için kullanıcı id'si oturuma konur.
                sessionAttrs.put("kullaniciId", k.getId());
            }
            return message;
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination == null) {
                return message;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionAttrs = (Map<String, Object>) accessor.getSessionAttributes();
            Long oturumSirketId = sessionAttrs != null ? (Long) sessionAttrs.get("sirketId") : null;
            Long kullaniciId = sessionAttrs != null ? (Long) sessionAttrs.get("kullaniciId") : null;

            Matcher m = SIRKET_TOPIC_PATTERN.matcher(destination);
            if (m.matches()) {
                Long aboneSirketId = Long.valueOf(m.group(2));
                if (oturumSirketId == null || !oturumSirketId.equals(aboneSirketId)) {
                    throw new MessageDeliveryException("Bu sirkete ait kanala abone olma yetkiniz yok");
                }
                return message;
            }

            // Genel sohbet kanalı (mesaj + silme olayı). Faz 3.1: /sil de doğrulanır.
            Matcher genel = SOHBET_GENEL_PATTERN.matcher(destination);
            Matcher genelSil = SOHBET_GENEL_SIL_PATTERN.matcher(destination);
            if (genel.matches() || genelSil.matches()) {
                Long aboneSirketId = Long.valueOf((genel.matches() ? genel : genelSil).group(1));
                if (oturumSirketId == null || !oturumSirketId.equals(aboneSirketId)) {
                    throw new MessageDeliveryException("Bu sirkete ait kanala abone olma yetkiniz yok");
                }
                return message;
            }

            // Oda kanalları: mesaj + sil + yazıyor. Şirket VE oda üyeliği doğrulanır.
            Matcher odaM = ODA_TOPIC_PATTERN.matcher(destination);
            Matcher odaSilM = ODA_SIL_TOPIC_PATTERN.matcher(destination);
            Matcher yaziyorM = ODA_YAZIYOR_TOPIC_PATTERN.matcher(destination);
            if (odaM.matches() || odaSilM.matches() || yaziyorM.matches()) {
                Matcher eslesen = odaM.matches() ? odaM : (odaSilM.matches() ? odaSilM : yaziyorM);
                Long aboneSirketId = Long.valueOf(eslesen.group(1));
                Long odaId = Long.valueOf(eslesen.group(2));
                if (oturumSirketId == null || !oturumSirketId.equals(aboneSirketId)) {
                    throw new MessageDeliveryException("Bu sohbet odasına abone olma yetkiniz yok");
                }
                // Üyelik zorunlu: aynı şirkette olup odaya üye olmayan biri dinleyemez.
                if (kullaniciId == null
                        || !sohbetOdaUyeRepository.existsByOdaIdAndKullaniciId(odaId, kullaniciId)) {
                    throw new MessageDeliveryException("Bu sohbet odasının üyesi değilsiniz");
                }
                return message;
            }
        }
        return message;
    }
}
