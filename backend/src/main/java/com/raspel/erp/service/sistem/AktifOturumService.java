package com.raspel.erp.service.sistem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.dto.sistem.AktifOturumDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Aktif oturum yönetimi. JWT stateless olsa da, girişte Redis'e bir oturum kaydı
 * yazılır; yönetici bu oturumları listeleyebilir ve tek tek sonlandırabilir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AktifOturumService {

    private static final String SESSION_KEY = "session:";
    private static final String SESSION_USER_KEY = "session:user:";
    private static final String REVOKED_KEY = "session:revoked:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** Oturumu Redis'e kaydeder. TTL, token ömrü ile aynı tutulur. */
    public void oturumKaydet(String jti, Long kullaniciId, String kullaniciAdi, Long sirketId, String ip, Duration ttl) {
        if (jti == null || kullaniciId == null) return;
        try {
            AktifOturumDTO dto = AktifOturumDTO.builder()
                    .jti(jti).kullaniciId(kullaniciId).kullaniciAdi(kullaniciAdi)
                    .sirketId(sirketId).ip(ip)
                    .girisZamani(LocalDateTime.now())
                    .sonKullanim(LocalDateTime.now().plus(ttl))
                    .build();
            redisTemplate.opsForValue().set(SESSION_KEY + jti, objectMapper.writeValueAsString(dto), ttl);
            redisTemplate.opsForSet().add(SESSION_USER_KEY + kullaniciId, jti);
        } catch (Exception e) {
            log.warn("Oturum Redis'e kaydedilemedi: {}", e.getMessage());
        }
    }

    /** Kullanıcının aktif oturumlarını listeler (admin tüm kullanıcıları görebilir). */
    public List<AktifOturumDTO> aktifOturumlar(Long kullaniciId, boolean tumu) {
        List<AktifOturumDTO> sonuc = new ArrayList<>();
        try {
            List<String> jtiler = new ArrayList<>();
            if (tumu || kullaniciId == null) {
                // KEYS yerine SCAN kullanılır (production'da Redis'i bloklamaz).
                for (String anahtar : scanKeys(SESSION_KEY + "*")) {
                    jtiler.add(anahtar.substring(SESSION_KEY.length()));
                }
            } else {
                Set<String> set = redisTemplate.opsForSet().members(SESSION_USER_KEY + kullaniciId);
                if (set != null) jtiler.addAll(set);
            }
            for (String jti : jtiler) {
                String json = redisTemplate.opsForValue().get(SESSION_KEY + jti);
                if (json == null) continue;
                AktifOturumDTO dto = objectMapper.readValue(json, AktifOturumDTO.class);
                if (dto != null && (tumu || kullaniciId == null || kullaniciId.equals(dto.getKullaniciId()))) {
                    sonuc.add(dto);
                }
            }
        } catch (Exception e) {
            log.warn("Aktif oturumlar listelenemedi: {}", e.getMessage());
        }
        return sonuc;
    }

    /** Oturumu iptal eder: kaydı siler ve token'ı kara listeye alır. */
    public void oturumIptal(String jti) {
        if (jti == null) return;
        try {
            String json = redisTemplate.opsForValue().get(SESSION_KEY + jti);
            if (json != null) {
                AktifOturumDTO dto = objectMapper.readValue(json, AktifOturumDTO.class);
                if (dto != null && dto.getKullaniciId() != null) {
                    redisTemplate.opsForSet().remove(SESSION_USER_KEY + dto.getKullaniciId(), jti);
                }
            }
            redisTemplate.delete(SESSION_KEY + jti);
            // Kalan token süresi kadar kara listede tut (max 30 gün)
            redisTemplate.opsForValue().set(REVOKED_KEY + jti, "1", Duration.ofDays(30));
        } catch (Exception e) {
            log.warn("Oturum iptal edilemedi: {}", e.getMessage());
        }
    }

    /** Token'ın iptal edilip edilmediğini kontrol eder. */
    public boolean iptalEdilmis(String jti) {
        if (jti == null) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(REVOKED_KEY + jti));
        } catch (Exception e) {
            return false;
        }
    }

    /** Redis SCAN ile deseni eşleşen anahtarları güvenli şekilde döndürür (KEYS yerine). */
    private Set<String> scanKeys(String pattern) {
        Set<String> sonuc = new HashSet<>();
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(100).build())) {
            while (cursor.hasNext()) {
                sonuc.add(cursor.next());
            }
        } catch (Exception e) {
            log.warn("Redis SCAN başarısız: {}", e.getMessage());
        }
        return sonuc;
    }
}
