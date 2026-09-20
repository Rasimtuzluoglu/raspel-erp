package com.raspel.erp.config;

import com.raspel.erp.config.security.JwtUtil;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.service.sistem.AktifOturumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtChannelInterceptorTest {

    private JwtUtil jwtUtil;
    private KullaniciRepository kullaniciRepository;
    private AktifOturumService aktifOturumService;
    private JwtChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        kullaniciRepository = mock(KullaniciRepository.class);
        aktifOturumService = mock(AktifOturumService.class);
        interceptor = new JwtChannelInterceptor(jwtUtil, kullaniciRepository, aktifOturumService);
    }

    private Message<?> connectMesaji(Map<String, Object> sessionAttrs) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(sessionAttrs);
        accessor.setSessionId("s1");
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> subscribeMesaji(String destination, Map<String, Object> sessionAttrs) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSessionAttributes(sessionAttrs);
        accessor.setSessionId("s1");
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void connect_gecerliTokenIleKullaniciVeSirketBaglani() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("token", "gecerli-token");
        when(jwtUtil.validateToken("gecerli-token")).thenReturn(true);
        when(jwtUtil.getJtiFromToken("gecerli-token")).thenReturn("jti-1");
        when(jwtUtil.getUsernameFromToken("gecerli-token")).thenReturn("ali");
        when(jwtUtil.getSirketIdFromToken("gecerli-token")).thenReturn(5L);
        when(jwtUtil.getTokenVersionFromToken("gecerli-token")).thenReturn(0L);
        when(kullaniciRepository.findByUsername("ali")).thenReturn(Optional.of(Kullanici.builder()
                .id(1L).username("ali").role("USER").active(true).sirketId(5L).tokenVersion(0L).build()));
        when(aktifOturumService.iptalEdilmis("jti-1")).thenReturn(false);
        when(aktifOturumService.aktifOturumMu(1L, "jti-1")).thenReturn(true);

        interceptor.preSend(connectMesaji(attrs), null);

        assertEquals(5L, attrs.get("sirketId"));
    }

    @Test
    void connect_iptalEdilmisOturumReddedilir() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("token", "iptal-token");
        when(jwtUtil.validateToken("iptal-token")).thenReturn(true);
        when(jwtUtil.getJtiFromToken("iptal-token")).thenReturn("jti-x");
        when(aktifOturumService.iptalEdilmis("jti-x")).thenReturn(true);

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(connectMesaji(attrs), null));
    }

    @Test
    void subscribe_kendiSirketKanalinaIzinVerilir() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sirketId", 5L);
        Message<?> mesaj = subscribeMesaji("/topic/bildirimler/5", attrs);
        assertDoesNotThrow(() -> interceptor.preSend(mesaj, null));
    }

    @Test
    void subscribe_baskaSirketKanalinaReddedilir() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sirketId", 5L);
        Message<?> mesaj = subscribeMesaji("/topic/bildirimler/9", attrs);
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(mesaj, null));
    }

    @Test
    void subscribe_sohbetBaskaSirketKanalinaReddedilir() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sirketId", 5L);
        Message<?> mesaj = subscribeMesaji("/topic/sohbet/9", attrs);
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(mesaj, null));
    }

    @Test
    void subscribe_anonimOturumSirketKanalinaReddedilir() {
        Message<?> mesaj = subscribeMesaji("/topic/bildirimler/5", new HashMap<>());
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(mesaj, null));
    }

    @Test
    void subscribe_sirketDisiKanallaraDokunulmaz() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sirketId", 5L);
        Message<?> mesaj = subscribeMesaji("/topic/genel", attrs);
        assertDoesNotThrow(() -> interceptor.preSend(mesaj, null));
    }

    @Test
    void subscribe_odaYaziyorKanalinaIzinVerilir() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sirketId", 5L);
        Message<?> mesaj = subscribeMesaji("/topic/sohbet/oda/5/7/yaziyor", attrs);
        assertDoesNotThrow(() -> interceptor.preSend(mesaj, null));
    }

    @Test
    void subscribe_odaYaziyorBaskaSirketKanalinaReddedilir() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sirketId", 5L);
        Message<?> mesaj = subscribeMesaji("/topic/sohbet/oda/9/7/yaziyor", attrs);
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(mesaj, null));
    }
}
