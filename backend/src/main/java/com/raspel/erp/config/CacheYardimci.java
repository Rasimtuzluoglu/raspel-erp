package com.raspel.erp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Servisler arasi cache evict yardimcisi.
 * Bir servis baska bir servisin cache'inde tutulan veriyi degistirdiginde
 * ilgili cache'i temizlemek icin kullanilir (ornegin fatura kesildiginde
 * stok cache'i).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheYardimci {

    private final CacheManager cacheManager;

    public void temizle(String... cacheAdlari) {
        for (String ad : cacheAdlari) {
            try {
                Cache cache = cacheManager.getCache(ad);
                if (cache != null) {
                    cache.clear();
                }
            } catch (Exception e) {
                log.warn("Cache temizlenemedi [{}]: {}", ad, e.getMessage());
            }
        }
    }

    /**
     * Tek bir cache anahtarini temizler (tum cache'i degil).
     *
     * <p>{@code @CacheEvict(allEntries = true)} her islemde TUM tenant'larin
     * verisini siliyordu: bir sirketin stok hareketi baska sirketin cache'ini de
     * dusuruyor, sonraki istekte tum tablolar yeniden sorgulaniyor (thundering
     * herd). Anahtar bazli temizleme yalnızca ilgili girdiyi düşürür.
     */
    public void anahtarTemizle(String cacheAdi, Object anahtar) {
        temizle(cacheAdi, anahtar);
    }

    private void temizle(String cacheAdi, Object anahtar) {
        try {
            Cache cache = cacheManager.getCache(cacheAdi);
            if (cache != null) {
                cache.evict(anahtar);
            }
        } catch (Exception e) {
            log.warn("Cache anahtari temizlenemedi [{}:{}]: {}", cacheAdi, anahtar, e.getMessage());
        }
    }

    /**
     * <b>Commit SONRASI</b> cache temizleme.
     *
     * <p><b>Neden gerekli:</b> {@code @CacheEvict} metod döndüğünde, yani
     * transaction henüz COMMIT OLMADIKKEN çalışır. Bu iki hatalı sonuç doğurur:
     * <ol>
     *   <li><b>Rollback:</b> evict gerçekleşmiş olur ama veri değişmemiştir —
     *       zararsız ama gereksiz bir cache soğukluğu.</li>
     *   <li><b>Yarış (asıl tehlike):</b> evict ile commit arasındaki pencerede
     *       eşzamanlı bir okuma ESKİ (commit edilmemiş) veriyi okuyup cache'e
     *       yazar. Commit sonrası girdi hâlâ bayat kalır ve TTL boyunca yanlış
     *       veri servis edilir.</li>
     * </ol>
     * Commit sonrasına alınca temizleme yalnızca kalıcı olan değişikliği izler
     * ve yarış penceresi kapanır.
     *
     * <p>Transaction dışındaysa (test, scheduler) temizleme doğrudan yapılır.
     */
    public void commitSonrasiTemizle(String... cacheAdlari) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            temizle(cacheAdlari);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                temizle(cacheAdlari);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    log.debug("Transaction rollback oldu; cache temizlemesi atlandi: {}",
                            java.util.Arrays.toString(cacheAdlari));
                }
            }
        });
    }

    /**
     * Commit sonrası <b>anahtar bazlı</b> temizleme. Üstteki açıklamanın aynısı
     * geçerlidir; ek olarak tüm cache'i düşürmez.
     */
    public void commitSonrasiAnahtarTemizle(String cacheAdi, Object anahtar) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            anahtarTemizle(cacheAdi, anahtar);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                anahtarTemizle(cacheAdi, anahtar);
            }
        });
    }
}