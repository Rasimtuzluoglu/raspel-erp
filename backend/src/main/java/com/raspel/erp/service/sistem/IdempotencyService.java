package com.raspel.erp.service.sistem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Çok replikalı (yatay ölçeklenmiş) ortamda da geçerli idempotency kilidi.
 * Redis varsa {@code SET NX EX} ile dağıtık kilit kullanılır; Redis yoksa
 * (dev/test) aynı JVM içindeki yerel map'e düşer.
 *
 * <p>Değerler: {@code ISLENIYOR} (rezerve) veya {@code OK:<kayitId>} (tamamlandı).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private static final Duration VARSAYILAN_TTL = Duration.ofMinutes(10);
    private static final String ISLENIYOR = "ISLENIYOR";
    private static final String TAMAM = "OK:";

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    private final ConcurrentHashMap<String, YerelKayit> yerel = new ConcurrentHashMap<>();

    /** Anahtarı bu istek için rezerve eder. false ise başka bir istek işliyor/tamamladı. */
    public boolean deneKilit(String anahtar) {
        return deneKilit(anahtar, VARSAYILAN_TTL);
    }

    /**
     * Belirtilen TTL ile rezerve eder. Cevrimdisi kuyruk gibi saatler sonra
     * tekrar gonderilebilen para hareketleri icin uzun TTL kullanilir.
     */
    public boolean deneKilit(String anahtar, Duration ttl) {
        Duration sure = ttl == null || ttl.isNegative() ? VARSAYILAN_TTL : ttl;
        StringRedisTemplate redis = redisVarsa();
        if (redis != null) {
            try {
                Boolean alindi = redis.opsForValue().setIfAbsent(anahtar, ISLENIYOR, sure);
                return Boolean.TRUE.equals(alindi);
            } catch (Exception e) {
                log.warn("Idempotency Redis kilidi alinamadi, yerel kilide geciliyor: {}", e.getMessage());
            }
        }
        long sonKullanma = System.currentTimeMillis() + sure.toMillis();
        yerelTemizle();
        final boolean[] alindi = {false};
        yerel.compute(anahtar, (k, mevcut) -> {
            if (mevcut == null || mevcut.suresiDoldu()) {
                alindi[0] = true;
                return new YerelKayit(ISLENIYOR, sonKullanma);
            }
            return mevcut;
        });
        return alindi[0];
    }

    /** İşlem tamamlandıysa oluşan kaydın ID'sini döner. */
    public Optional<Long> tamamlananSonuc(String anahtar) {
        StringRedisTemplate redis = redisVarsa();
        if (redis != null) {
            try {
                String v = redis.opsForValue().get(anahtar);
                return parse(v);
            } catch (Exception e) {
                log.warn("Idempotency Redis okunamadı, yerel kayda bakılıyor: {}", e.getMessage());
            }
        }
        YerelKayit kayit = yerel.get(anahtar);
        if (kayit == null || kayit.suresiDoldu()) return Optional.empty();
        return parse(kayit.deger());
    }

    /** İşlemi başarıyla tamamlandı olarak işaretler. */
    public void tamamla(String anahtar, Long kayitId) {
        tamamla(anahtar, kayitId, VARSAYILAN_TTL);
    }

    /** Tamamlanan işlemi belirtilen TTL ile işaretler (çevrimdışı kuyruk için uzun TTL). */
    public void tamamla(String anahtar, Long kayitId, Duration ttl) {
        Duration sure = ttl == null || ttl.isNegative() ? VARSAYILAN_TTL : ttl;
        String deger = TAMAM + kayitId;
        StringRedisTemplate redis = redisVarsa();
        if (redis != null) {
            try {
                redis.opsForValue().set(anahtar, deger, sure);
                yerel.remove(anahtar);
                return;
            } catch (Exception e) {
                log.warn("Idempotency Redis yazılamadı, yerel kayda yazılıyor: {}", e.getMessage());
            }
        }
        yerel.put(anahtar, new YerelKayit(deger, System.currentTimeMillis() + sure.toMillis()));
    }

    /** İşlem başarısız olduysa rezervasyonu serbest bırakır (sonraki deneme çalışsın). */
    public void serbestBirak(String anahtar) {
        StringRedisTemplate redis = redisVarsa();
        if (redis != null) {
            try {
                redis.delete(anahtar);
            } catch (Exception e) {
                log.warn("Idempotency Redis kilidi bırakılamadı: {}", e.getMessage());
            }
        }
        yerel.remove(anahtar);
    }

    private Optional<Long> parse(String deger) {
        if (deger == null || !deger.startsWith(TAMAM)) return Optional.empty();
        try {
            return Optional.of(Long.valueOf(deger.substring(TAMAM.length())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private StringRedisTemplate redisVarsa() {
        try {
            return redisProvider.getIfAvailable();
        } catch (Exception e) {
            return null;
        }
    }

    private void yerelTemizle() {
        if (yerel.size() > 10_000) {
            yerel.entrySet().removeIf(e -> e.getValue().suresiDoldu());
        }
    }

    private record YerelKayit(String deger, long sonKullanma) {
        boolean suresiDoldu() {
            return System.currentTimeMillis() >= sonKullanma;
        }
    }
}
