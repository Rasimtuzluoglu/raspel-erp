package com.raspel.erp.service.sistem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.dto.sistem.LoginResponse;
import com.raspel.erp.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Telefonla QR oturum onayi.
 *
 * <p>Akis: parola + 2FA adiminda masaustu QR uretir; telefon (oturum acmis)
 * QR'i okutup onaylar; masaustu poll eder ve girisi tamamlar. QR tek kullanim,
 * 120 sn gecerlidir ve yalnizca ayni kullanici onaylayabilir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QrOturumService {

    private static final String OTURUM_PREFIX = "qr:oturum:";
    private static final String GIRIS_PREFIX = "qr:giris:";
    private static final Duration TTL = Duration.ofSeconds(120);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final KullaniciService kullaniciService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final QRService qrService;

    @Value("${app.public-url:http://localhost}")
    private String publicUrl;

    private final Map<String, QrKayit> bellek = new ConcurrentHashMap<>();

    record QrKayit(
            String qrToken,
            String girisToken,
            Long kullaniciId,
            String durum,
            long olusturma,
            long sonGecerlilik,
            String ip,
            String userAgent,
            Long onaylayanKullaniciId,
            Long onayZamani) {
    }

    public Map<String, Object> baslat(String girisToken, String ip, String userAgent) {
        if (girisToken == null || girisToken.isBlank()) {
            throw new BusinessException("Giriş oturumu bulunamadı");
        }
        Long kullaniciId = kullaniciService.bekleyenKullaniciId(girisToken);
        if (!kullaniciService.bekleyenIkiFaktorAktif(girisToken)) {
            throw new BusinessException("QR ile onay yalnızca iki adımlı doğrulama açıkken kullanılabilir");
        }

        byte[] ham = new byte[32];
        RANDOM.nextBytes(ham);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(ham);
        long simdi = System.currentTimeMillis();
        QrKayit kayit = new QrKayit(token, girisToken, kullaniciId, "BEKLIYOR", simdi,
                simdi + TTL.toMillis(), ip, userAgent, null, null);
        kaydet(OTURUM_PREFIX + token, kayit);
        kaydet(GIRIS_PREFIX + girisToken, kayit);

        Map<String, Object> cevap = new LinkedHashMap<>();
        cevap.put("token", token);
        cevap.put("qrUrl", temelUrl() + "/qr-onay/" + token);
        cevap.put("expiresIn", TTL.toSeconds());
        return cevap;
    }

    public Map<String, Object> durum(String token) {
        QrKayit kayit = oku(OTURUM_PREFIX + token);
        Map<String, Object> cevap = new LinkedHashMap<>();
        if (kayit == null) {
            cevap.put("durum", "SURESI_DOLDU");
            cevap.put("kalanSn", 0);
            return cevap;
        }
        cevap.put("durum", kayit.durum());
        cevap.put("kalanSn", Math.max(0, (kayit.sonGecerlilik() - System.currentTimeMillis()) / 1000));
        return cevap;
    }

    public byte[] gorsel(String token) {
        QrKayit kayit = oku(OTURUM_PREFIX + token);
        if (kayit == null) {
            throw new BusinessException("QR kodunun süresi doldu");
        }
        return qrService.qrPng(temelUrl() + "/qr-onay/" + token, 320);
    }

    public void onayla(String token, Long kullaniciId) {
        QrKayit kayit = oku(OTURUM_PREFIX + token);
        if (kayit == null) {
            throw new BusinessException("QR kodunun süresi doldu");
        }
        if (!"BEKLIYOR".equals(kayit.durum())) {
            throw new BusinessException("Bu QR kod daha önce işlendi");
        }
        if (!kayit.kullaniciId().equals(kullaniciId)) {
            throw new BusinessException("Bu giriş isteği başka bir kullanıcıya ait");
        }
        QrKayit guncel = new QrKayit(kayit.qrToken(), kayit.girisToken(), kayit.kullaniciId(), "ONAYLANDI",
                kayit.olusturma(), kayit.sonGecerlilik(), kayit.ip(), kayit.userAgent(),
                kullaniciId, System.currentTimeMillis());
        kaydet(OTURUM_PREFIX + token, guncel);
        kaydet(GIRIS_PREFIX + kayit.girisToken(), guncel);
    }

    public void reddet(String token, Long kullaniciId) {
        QrKayit kayit = oku(OTURUM_PREFIX + token);
        if (kayit == null) {
            throw new BusinessException("QR kodunun süresi doldu");
        }
        if (!"BEKLIYOR".equals(kayit.durum())) {
            throw new BusinessException("Bu QR kod daha önce işlendi");
        }
        if (!kayit.kullaniciId().equals(kullaniciId)) {
            throw new BusinessException("Bu giriş isteği başka bir kullanıcıya ait");
        }
        QrKayit guncel = new QrKayit(kayit.qrToken(), kayit.girisToken(), kayit.kullaniciId(), "REDDEDILDI",
                kayit.olusturma(), kayit.sonGecerlilik(), kayit.ip(), kayit.userAgent(),
                kullaniciId, System.currentTimeMillis());
        kaydet(OTURUM_PREFIX + token, guncel);
        kaydet(GIRIS_PREFIX + kayit.girisToken(), guncel);
    }

    public LoginResponse tamamla(String girisToken) {
        QrKayit kayit = oku(GIRIS_PREFIX + girisToken);
        if (kayit == null) {
            throw new BusinessException("QR onayı bulunamadı veya süresi doldu");
        }
        if ("REDDEDILDI".equals(kayit.durum())) {
            sil(girisToken, null);
            throw new BusinessException("Giriş isteği telefonunuzdan reddedildi");
        }
        if (!"ONAYLANDI".equals(kayit.durum())) {
            throw new BusinessException("Giriş isteği henüz onaylanmadı");
        }
        if (kayit.onaylayanKullaniciId() == null
                || !kayit.onaylayanKullaniciId().equals(kayit.kullaniciId())) {
            throw new BusinessException("QR onayı doğrulanamadı");
        }
        LoginResponse cevap = kullaniciService.qrIleGirisTamamla(girisToken);
        sil(girisToken, kayit);
        return cevap;
    }

    // ------------------------------------------------------------------

    private String temelUrl() {
        String url = publicUrl == null ? "http://localhost" : publicUrl.trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private void kaydet(String anahtar, QrKayit kayit) {
        bellek.put(anahtar, kayit);
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(anahtar,
                        objectMapper.writeValueAsString(kayit), TTL);
            }
        } catch (Exception e) {
            log.warn("QR oturumu Redis'e yazılamadı, bellekte tutulacak: {}", e.getMessage());
        }
    }

    private QrKayit oku(String anahtar) {
        try {
            if (redisTemplate != null) {
                String json = redisTemplate.opsForValue().get(anahtar);
                if (json != null) {
                    QrKayit kayit = objectMapper.readValue(json, QrKayit.class);
                    if (kayit.sonGecerlilik() < System.currentTimeMillis()) return null;
                    return kayit;
                }
            }
        } catch (Exception e) {
            log.warn("QR oturumu Redis'ten okunamadı: {}", e.getMessage());
        }
        QrKayit kayit = bellek.get(anahtar);
        if (kayit == null || kayit.sonGecerlilik() < System.currentTimeMillis()) return null;
        return kayit;
    }

    private void sil(String girisToken, QrKayit kayit) {
        if (kayit != null) {
            bellek.remove(OTURUM_PREFIX + kayit.qrToken());
            try {
                if (redisTemplate != null) redisTemplate.delete(OTURUM_PREFIX + kayit.qrToken());
            } catch (Exception e) {
                // yoksay
            }
        }
        bellek.remove(GIRIS_PREFIX + girisToken);
        try {
            if (redisTemplate != null) redisTemplate.delete(GIRIS_PREFIX + girisToken);
        } catch (Exception e) {
            // yoksay
        }
    }
}
