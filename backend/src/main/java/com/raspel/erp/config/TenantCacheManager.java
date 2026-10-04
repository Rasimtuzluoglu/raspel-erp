package com.raspel.erp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tenant izole cache adlandırması.
 *
 * <p><b>Sorun:</b> {@code lookup} ve {@code dashboard} cache'leri tüm şirketler
 * arasında PAYLAŞILIYOR (tek fiziksel Redis adı). 44 ayrı
 * {@code @CacheEvict(value = "lookup", allEntries = true)} çağrısı vardı: bir
 * şirketin tek bir yazma işlemi (ör. yeni kategori) TÜM şirketlerin lookup
 * cache'ini düşürüyordu. Her istek donem/kategori/şube/adres tablolarını yeniden
 * sorguluyor (thundering herd) ve Redis yükü gereksiz yere artıyordu.
 *
 * <p><b>Çözüm:</b> Tenant kapsamlı sayılan cache'ler için fiziksel ad
 * {@code <ad>:t<sirketId>} olur. {@link ScanRedisCacheWriter#clean} zaten
 * fiziksel adı kullandığı için {@code clear()} yalnızca o tenant'ın girdilerini
 * siler. Anahtar (key) tarafına dokunulmaz; okuma ve yazma aynı adı kullandığı
 * için tutarlıdır.
 *
 * <p><b>Tenant bağlamı yoksa</b> (scheduler, startup, konsol) ad şemasız kalır —
 * tüm bağlam-ötesi kullanım paylaşılan adı kullanır. Böylece bir job'un
 * temizliği tüm tenant'ları etkiler (aynı davranış) ama <b>HTTP isteklerinde</b>
 * artık sadece ilgili şirket düşer.
 *
 * <p>Not: Tenant kapsamlı olmayan cache'ler ({@code dovizKurlari} gibi global
 * veri) listede bulunmaz ve davranışları değişmez.
 */
@Slf4j
public class TenantCacheManager implements CacheManager {

    /** Anahtar/zaman boyunca tenant'a bağlı olmayan cache'ler. */
    private static final Set<String> TENANT_KAPSAMLI = Set.of("lookup", "dashboard");

    private final TenantChecker tenantChecker;
    private final CacheManager delegate;

    /** tenantId -> fiziksel ad -> Cache */
    private final Map<Long, Map<String, Cache>> tenantCache = new ConcurrentHashMap<>();

    public TenantCacheManager(TenantChecker tenantChecker, CacheManager delegate) {
        this.tenantChecker = tenantChecker;
        this.delegate = delegate;
    }

    @Override
    public Cache getCache(String name) {
        if (name == null || !TENANT_KAPSAMLI.contains(name)) {
            return delegate.getCache(name);
        }
        Long sirketId = tenantChecker.getCurrentSirketId();
        if (sirketId == null) {
            // Tenant baglami yok: ortak ad kullanilir.
            return delegate.getCache(name);
        }
        return tenantCache
                .computeIfAbsent(sirketId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(name, n -> {
                    Cache cache = delegate.getCache(n + ":t" + sirketId);
                    if (cache == null) {
                        log.warn("Tenant cache olusturulamadi: {}:t{}", n, sirketId);
                    }
                    return cache;
                });
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }

    /**
 * Tüm tenant cache'lerini düşürmek için (cache temizleme uçları, testler).
     * {@link CacheYardimci#temizle} yalnızca aktif tenant'ın adlarını kullanır.
     */
    public void tumTenantCacheTemizle() {
        TENANT_KAPSAMLI.forEach(ad -> tenantCache.values()
                .forEach(m -> m.values().forEach(Cache::clear)));
        tenantCache.clear();
    }
}