package com.raspel.erp.service.sistem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.dto.sistem.LoginResponse;
import com.raspel.erp.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrOturumServiceTest {

    @Mock private KullaniciService kullaniciService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private QRService qrService;

    private QrOturumService service;

    @BeforeEach
    void kur() {
        service = new QrOturumService(kullaniciService, redisTemplate, new ObjectMapper(), qrService);
        ReflectionTestUtils.setField(service, "publicUrl", "https://erp.example.com");
    }

    @Test
    void baslat_qrTokeniVeUrlDoner() {
        when(kullaniciService.bekleyenKullaniciId("giris-token")).thenReturn(1L);
        when(kullaniciService.bekleyenIkiFaktorAktif("giris-token")).thenReturn(true);

        Map<String, Object> sonuc = service.baslat("giris-token", "10.0.0.1", "Mozilla");

        assertNotNull(sonuc.get("token"));
        assertEquals("https://erp.example.com/qr-onay/" + sonuc.get("token"), sonuc.get("qrUrl"));
        assertEquals(120L, sonuc.get("expiresIn"));
    }

    @Test
    void baslat_ikiFaktorKapaliysaReddeder() {
        when(kullaniciService.bekleyenKullaniciId("giris-token")).thenReturn(1L);
        when(kullaniciService.bekleyenIkiFaktorAktif("giris-token")).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.baslat("giris-token", "ip", "ua"));
    }

    @Test
    void onayla_veDurum_akisi() {
        when(kullaniciService.bekleyenKullaniciId("giris-token")).thenReturn(1L);
        when(kullaniciService.bekleyenIkiFaktorAktif("giris-token")).thenReturn(true);
        String token = (String) service.baslat("giris-token", "ip", "ua").get("token");

        assertEquals("BEKLIYOR", service.durum(token).get("durum"));
        service.onayla(token, 1L);
        assertEquals("ONAYLANDI", service.durum(token).get("durum"));
    }

    @Test
    void onayla_baskaKullaniciyiReddeder() {
        when(kullaniciService.bekleyenKullaniciId("giris-token")).thenReturn(1L);
        when(kullaniciService.bekleyenIkiFaktorAktif("giris-token")).thenReturn(true);
        String token = (String) service.baslat("giris-token", "ip", "ua").get("token");

        assertThrows(BusinessException.class, () -> service.onayla(token, 2L));
    }

    @Test
    void tamamla_onaylanmisGirisiBitirirVeTekKullanimliktir() {
        when(kullaniciService.bekleyenKullaniciId("giris-token")).thenReturn(1L);
        when(kullaniciService.bekleyenIkiFaktorAktif("giris-token")).thenReturn(true);
        String token = (String) service.baslat("giris-token", "ip", "ua").get("token");
        service.onayla(token, 1L);

        LoginResponse beklenen = LoginResponse.builder().username("testuser").build();
        when(kullaniciService.qrIleGirisTamamla("giris-token")).thenReturn(beklenen);

        assertSame(beklenen, service.tamamla("giris-token"));
        assertThrows(BusinessException.class, () -> service.tamamla("giris-token"));
    }

    @Test
    void tamamla_onaylanmadanReddeder() {
        when(kullaniciService.bekleyenKullaniciId("giris-token")).thenReturn(1L);
        when(kullaniciService.bekleyenIkiFaktorAktif("giris-token")).thenReturn(true);
        service.baslat("giris-token", "ip", "ua");

        assertThrows(BusinessException.class, () -> service.tamamla("giris-token"));
    }

    @Test
    void reddet_sonrasiTamamlaReddeder() {
        when(kullaniciService.bekleyenKullaniciId("giris-token")).thenReturn(1L);
        when(kullaniciService.bekleyenIkiFaktorAktif("giris-token")).thenReturn(true);
        String token = (String) service.baslat("giris-token", "ip", "ua").get("token");
        service.reddet(token, 1L);

        assertThrows(BusinessException.class, () -> service.tamamla("giris-token"));
    }

    @Test
    void gorsel_qrServisiniKullanir() {
        when(kullaniciService.bekleyenKullaniciId("giris-token")).thenReturn(1L);
        when(kullaniciService.bekleyenIkiFaktorAktif("giris-token")).thenReturn(true);
        String token = (String) service.baslat("giris-token", "ip", "ua").get("token");
        byte[] png = new byte[]{1, 2, 3};
        when(qrService.qrPng("https://erp.example.com/qr-onay/" + token, 320)).thenReturn(png);

        assertArrayEquals(png, service.gorsel(token));
    }
}
