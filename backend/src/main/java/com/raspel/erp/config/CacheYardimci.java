package com.raspel.erp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

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
}
