package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.PushAbonelikDTO;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.PushAbonelik;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.PushAbonelikRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * PWA Web Push yonetimi. VAPID anahtarlari tanimli degilse sessizce devre disi kalir;
 * uygulamanin geri kalani etkilenmez.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushService {

    private final PushAbonelikRepository pushAbonelikRepository;
    private final KullaniciRepository kullaniciRepository;

    @Value("${VAPID_PUBLIC_KEY:}")
    private String vapidPublicKey;

    @Value("${VAPID_PRIVATE_KEY:}")
    private String vapidPrivateKey;

    @Value("${VAPID_SUBJECT:mailto:admin@raspel-erp.com}")
    private String vapidSubject;

    private nl.martijndwars.webpush.PushService sender;
    private boolean hazir = false;

    @PostConstruct
    void baslat() {
        if (bosMu(vapidPublicKey) || bosMu(vapidPrivateKey)) {
            log.warn("VAPID anahtarlari tanimli degil; Web Push devre disi.");
            return;
        }
        try {
            if (java.security.Security.getProvider("BC") == null) {
                java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            }
            this.sender = new nl.martijndwars.webpush.PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
            this.hazir = true;
            log.info("Web Push aktif (VAPID yapilandirildi).");
        } catch (Exception e) {
            log.warn("Web Push baslatilamadi: {}", e.getMessage());
        }
    }

    public boolean aktif() {
        return hazir;
    }

    public String publicKey() {
        return vapidPublicKey;
    }

    @Transactional
    public PushAbonelikDTO aboneOl(PushAbonelikDTO dto, Long kullaniciId, Long sirketId, String userAgent) {
        if (dto == null || bosMu(dto.getEndpoint()) || dto.getKeys() == null
                || bosMu(dto.getKeys().getP256dh()) || bosMu(dto.getKeys().getAuth())) {
            throw new BusinessException("Gecersiz push aboneligi");
        }
        PushAbonelik abonelik = pushAbonelikRepository.findByEndpoint(dto.getEndpoint())
                .orElseGet(PushAbonelik::new);
        abonelik.setEndpoint(dto.getEndpoint());
        abonelik.setP256dh(dto.getKeys().getP256dh());
        abonelik.setAuth(dto.getKeys().getAuth());
        abonelik.setKullaniciId(kullaniciId);
        abonelik.setSirketId(sirketId);
        abonelik.setUserAgent(kisalt(userAgent, 500));
        pushAbonelikRepository.save(abonelik);
        log.info("Push aboneligi kaydedildi -> sirketId={}, kullaniciId={}", sirketId, kullaniciId);
        return dto;
    }

    @Transactional
    public void aboneSil(String endpoint) {
        if (bosMu(endpoint)) {
            return;
        }
        pushAbonelikRepository.deleteByEndpoint(endpoint);
        log.info("Push aboneligi silindi.");
    }

    /** Sirkete ait tum aboneliklere bildirim gonderir; gecersiz abonelikleri temizler. */
    public int gonder(Long sirketId, String baslik, String mesaj, String url) {
        return gonder(sirketId, null, baslik, mesaj, url);
    }

    /** Tur bazli: kullanicinin bildirim tercihleri bu turu kapsamiyorsa o abonelik atlanir. */
    public int gonder(Long sirketId, String tur, String baslik, String mesaj, String url) {
        if (!hazir || sirketId == null) {
            return 0;
        }
        return gonderAbonelikler(pushAbonelikRepository.findBySirketId(sirketId), tur, baslik, mesaj, url);
    }

    /** Tek kullaniciya ait tum aboneliklere bildirim gonderir. */
    public int gonderKullanici(Long kullaniciId, String baslik, String mesaj, String url) {
        if (!hazir || kullaniciId == null) {
            return 0;
        }
        return gonderAbonelikler(pushAbonelikRepository.findByKullaniciId(kullaniciId), null, baslik, mesaj, url);
    }

    private int gonderAbonelikler(List<PushAbonelik> abonelikler, String tur, String baslik, String mesaj, String url) {
        if (abonelikler == null || abonelikler.isEmpty()) {
            return 0;
        }
        Map<Long, String> tercihMap = tercihleriYukle(abonelikler);
        String payload = payloadOlustur(baslik, mesaj, url);
        int gonderilen = 0;
        for (PushAbonelik a : abonelikler) {
            if (a.getKullaniciId() != null && !turIzinli(tercihMap.get(a.getKullaniciId()), tur)) {
                continue;
            }
            try {
                Subscription subscription = new Subscription(a.getEndpoint(),
                        new Subscription.Keys(a.getP256dh(), a.getAuth()));
                HttpResponse response = sender.send(new Notification(subscription, payload));
                int kod = response.getStatusLine().getStatusCode();
                if (kod >= 200 && kod < 300) {
                    gonderilen++;
                } else if (kod == 404 || kod == 410) {
                    pushAbonelikRepository.delete(a);
                    log.info("Gecersiz push aboneligi silindi ({}).", kod);
                } else {
                    log.warn("Push gonderilemedi ({}).", kod);
                }
            } catch (Exception e) {
                log.warn("Push gonderim hatasi: {}", e.getMessage());
            }
        }
        return gonderilen;
    }

    private Map<Long, String> tercihleriYukle(List<PushAbonelik> abonelikler) {
        List<Long> ids = abonelikler.stream().map(PushAbonelik::getKullaniciId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return kullaniciRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Kullanici::getId,
                        k -> k.getBildirimTercihleri() != null ? k.getBildirimTercihleri() : ""));
    }

    /**
     * Bos tercih listesi = tum bildirim tipleri acik. Aksi halde yalnizca listede
     * gecen tipler gonderilir (whitelist).
     */
    public static boolean turIzinli(String tercihlerHam, String tur) {
        if (tur == null || tur.isBlank()) {
            return true;
        }
        if (tercihlerHam == null || tercihlerHam.isBlank()) {
            return true;
        }
        return java.util.Arrays.stream(tercihlerHam.split(","))
                .map(String::trim)
                .anyMatch(t -> t.equalsIgnoreCase(tur));
    }

    private String payloadOlustur(String baslik, String mesaj, String url) {
        return "{\"title\":" + json(baslik) + ",\"body\":" + json(mesaj)
                + ",\"url\":" + json(url != null ? url : "/") + "}";
    }

    private String json(String s) {
        if (s == null) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    private static boolean bosMu(String s) {
        return s == null || s.isBlank();
    }

    private static String kisalt(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
