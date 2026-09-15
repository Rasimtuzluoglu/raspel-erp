package com.raspel.erp.controller.sistem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SohbetWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/sohbet/oda/yaziyor")
    public void yaziyor(StompHeaderAccessor accessor, YaziyorMesaj msg) {
        if (accessor == null || msg == null || msg.odaId() == null) return;
        // Tenant (şirket) bağlamı istemciden alınmaz; bağlantı kurulurken geçerli JWT'den
        // doğrulanan oturum değişkeninden türetilir (JwtChannelInterceptor CONNECT aşamasında yazar).
        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        Long oturumSirketId = sessionAttrs != null ? (Long) sessionAttrs.get("sirketId") : null;
        if (oturumSirketId == null) {
            log.warn("Yaziyor mesaji gecersiz oturum baglami nedeniyle atlandi (sirketId yok).");
            return;
        }
        messagingTemplate.convertAndSend(
                "/topic/sohbet/oda/" + oturumSirketId + "/" + msg.odaId() + "/yaziyor",
                new YaziyorMesaj(oturumSirketId, msg.odaId(), msg.kullaniciAd()));
    }

    public record YaziyorMesaj(Long sirketId, Long odaId, String kullaniciAd) {}
}
