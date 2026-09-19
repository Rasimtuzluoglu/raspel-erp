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
    private static final Duration VARSAYILAN_IPTAL_TTL = Duration.ofDays(1);

    /**
     * Redis erişilemezse yerel olarak iptal edilen token'ların kısa süreli tutulduğu
     * liste (jti -> bitiş epoch ms). Redis kesintisinde iptallerin büsbütün kaybolmasını
     * engeller; sınırsız büyümeyi önlemek için üst sınır uygulanır.
     */
    private final java.util.concurrent.ConcurrentMap<String, Long> yerelIptaller = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int YEREL_IPTAL_LIMIT = 10_000;

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
        Duration kalan = VARSAYILAN_IPTAL_TTL;
        try {
            String json = redisTemplate.opsForValue().get(SESSION_KEY + jti);
            if (json != null) {
                AktifOturumDTO dto = objectMapper.readValue(json, AktifOturumDTO.class);
                if (dto != null && dto.getKullaniciId() != null) {
                    redisTemplate.opsForSet().remove(SESSION_USER_KEY + dto.getKullaniciId(), jti);
                }
                if (dto != null && dto.getSonKullanim() != null) {
                    // Kara liste, token'in gercek bitis zamanina kadar tutulur.
                    Duration hesaplanan = Duration.between(LocalDateTime.now(), dto.getSonKullanim());
                    if (!hesaplanan.isNegative() && !hesaplanan.isZero()) {
                        kalan = hesaplanan;
                    }
                }
            }
            redisTemplate.delete(SESSION_KEY + jti);
            redisTemplate.opsForValue().set(REVOKED_KEY + jti, "1", kalan);
        } catch (Exception e) {
            log.warn("Oturum iptal kaydı Redis'e yazılamadı, yerel listeye alındı: {}", e.getMessage());
        }
        yerelIptalEkle(jti, kalan);
    }

    private void yerelIptalEkle(String jti, Duration ttl) {
        try {
            if (yerelIptaller.size() >= YEREL_IPTAL_LIMIT) {
                long now = System.currentTimeMillis();
                yerelIptaller.entrySet().removeIf(e -> e.getValue() < now);
                if (yerelIptaller.size() >= YEREL_IPTAL_LIMIT) {
                    // Hâlâ doluysa en eski girdileri temizle
                    yerelIptaller.keySet().stream().limit(yerelIptaller.size() - (YEREL_IPTAL_LIMIT / 2))
                            .forEach(yerelIptaller::remove);
                }
            }
            yerelIptaller.put(jti, System.currentTimeMillis() + ttl.toMillis());
        } catch (Exception ignored) {
            /* yoksay */
        }
    }

    /** Oturumu jti'ye göre getirir (yoksa null). */
    public AktifOturumDTO oturumGetir(String jti) {
        if (jti == null) return null;
        try {
            String json = redisTemplate.opsForValue().get(SESSION_KEY + jti);
            return json == null ? null : objectMapper.readValue(json, AktifOturumDTO.class);
        } catch (Exception e) {
            log.warn("Oturum okunamadı: {}", e.getMessage());
            return null;
        }
    }

    /** Token'ın iptal edilip edilmediğini kontrol eder. */
    public boolean iptalEdilmis(String jti) {
        if (jti == null) return false;
        // Önce yerel (Redis kesintisinde de geçerli) iptal listesine bakılır.
        try {
            Long bitis = yerelIptaller.get(jti);
            if (bitis != null) {
                if (bitis >= System.currentTimeMillis()) return true;
                yerelIptaller.remove(jti);
            }
        } catch (Exception ignored) {
            /* yoksay */
        }
        if (redisTemplate == null) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(REVOKED_KEY + jti));
        } catch (Exception e) {
            // Redis erişilemezse yalnızca yerel iptal listesi uygulanır; aksi halde
            // Redis kesintisi tüm oturumları kilitleyeceği için burada fail-open kalınır.
            log.warn("Iptal kontrolu yapilamadi (yerel liste kullanildi): {}", e.getMessage());
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
