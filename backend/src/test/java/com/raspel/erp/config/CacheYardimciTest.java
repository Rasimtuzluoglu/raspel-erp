package com.raspel.erp.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Cache evict zamanlaması ve tenant izolasyonu testleri.
 *
 * <p><b>Evict neden commit sonrasına alınmalı?</b> {@code @CacheEvict} metot
 * döndüğünde çalışır; transaction henüz commit OLMAMIŞTIR. Evict ile commit
 * arasındaki pencerede eşzamanlı bir okuma ESKİ (commit edilmemiş) veriyi
 * cache'e yazar ve commit sonrası girdi TTL boyunca bayat kalır.
 */
class CacheYardimciTest {

    /**
 * Dinamik fiziksel adlari (lookup:t4 gibi) ureten temel yonlendirici.
 * RedisCacheManager gibi her ada TEK bir Cache ornegi dondurur ve veri tutar.
 */
private final CacheManager temelYonlendirici = new CacheManager() {
        private final java.util.Map<String, Cache> ornekler = new java.util.concurrent.ConcurrentHashMap<>();
        @Override public Cache getCache(String name) {
            return ornekler.computeIfAbsent(name, KayitliCache::new);
        }
        @Override public java.util.Collection<String> getCacheNames() { return ornekler.keySet(); }
    };

/** Temizleme cagrilarini sayan sahte cache (Mockito yerine: gercek cache mock'lanamaz). */
    static final class KayitliCache implements Cache {
        final String ad;
        final AtomicInteger clearSayisi = new AtomicInteger();
        final java.util.List<Object> silinenAnahtarlar = new java.util.ArrayList<>();
        private final java.util.Map<Object, Object> veri = new java.util.concurrent.ConcurrentHashMap<>();

        KayitliCache(String ad) { this.ad = ad; }

        @Override public String getName() { return ad; }
        @Override public Object getNativeCache() { return this; }

        @Override public ValueWrapper get(Object key) {
            Object v = veri.get(key);
            return v == null ? null : () -> v;
        }
        @Override public <T> T get(Object key, Class<T> type) {
            Object v = veri.get(key);
            return type.isInstance(v) ? type.cast(v) : null;
        }
        @Override public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            try {
                @SuppressWarnings("unchecked")
                T v = (T) valueLoader.call();
                veri.put(key, v);
                return v;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        @Override public void put(Object key, Object value) { veri.put(key, value); }
        @Override public ValueWrapper putIfAbsent(Object key, Object value) {
            Object mevcut = veri.putIfAbsent(key, value);
            return mevcut == null ? () -> value : () -> mevcut;
        }
        @Override public void evict(Object key) { silinenAnahtarlar.add(key); veri.remove(key); }
        @Override public boolean evictIfPresent(Object key) { silinenAnahtarlar.add(key); return veri.remove(key) != null; }
        @Override public void clear() { clearSayisi.incrementAndGet(); veri.clear(); }
        @Override public boolean invalidate() { clear(); return true; }
    }

    private final java.util.Map<String, KayitliCache> kayitli = new java.util.HashMap<>();
    private final CacheManager cacheManager = new CacheManager() {
        @Override public Cache getCache(String name) { return kayitli.computeIfAbsent(name, KayitliCache::new); }
        @Override public java.util.Collection<String> getCacheNames() { return kayitli.keySet(); }
    };

    private KayitliCache stoklar() {
        cacheManager.getCache("stoklar");
        return kayitli.get("stoklar");
    }
    private final CacheYardimci cacheYardimci = new CacheYardimci(cacheManager);

@AfterEach
    void sonraTemizle() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void transactionBaslat() {
        TransactionSynchronizationManager.initSynchronization();
    }

    /** Transaction yoksa temizleme anında yapılır (test/scheduler yolu). */
    @Test
    void transactionDisindaAnindaTemizler() {
        cacheYardimci.commitSonrasiTemizle("stoklar");

assertEquals(1, stoklar().clearSayisi.get());
        assertFalse(TransactionSynchronizationManager.isSynchronizationActive(),
                "Transaction disinda senkronizasyon kaydedilmemeli");
    }

    /** Transaction içinde temizleme ERTELEMEYE alınır, commit'te yapılır. */
    @Test
    void transactionIcindeCommitOncesiTemizlemez() {
        transactionBaslat();

        cacheYardimci.commitSonrasiTemizle("stoklar");

        assertEquals(0, stoklar().clearSayisi.get());
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
    }

    /** Commit sonrası temizleme gerçekleşir. */
    @Test
    void commitSonrasiTemizler() {
        transactionBaslat();
        cacheYardimci.commitSonrasiTemizle("stoklar", "dashboard");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCommit());

        assertTrue(stoklar().clearSayisi.get() >= 1);
    }

    /** Rollback'te temizleme yapılmaz (veri değişmedi). */
    @Test
    void rollbackteTemizlemez() {
        transactionBaslat();
        cacheYardimci.commitSonrasiTemizle("stoklar");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertEquals(0, stoklar().clearSayisi.get());
    }

    /** Tamamlanma durumu rollback ise afterCommit'in TETIKLENMEDIGI doğrulanır. */
    @Test
    void rollbackSonrasiAfterCommitCagrilmaz() {
        transactionBaslat();
        cacheYardimci.commitSonrasiTemizle("stoklar");

        var sync = TransactionSynchronizationManager.getSynchronizations().get(0);
        sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        assertEquals(0, stoklar().clearSayisi.get());
    }

    /** Anahtar bazlı temizleme tüm cache'i düşürmez. */
    @Test
    void anahtarBazliTemizlemeTumCacheiDusurmez() {
        transactionBaslat();
        cacheYardimci.commitSonrasiAnahtarTemizle("stoklar", "s4:42");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCommit());

        assertEquals(0, stoklar().clearSayisi.get());
        assertTrue(stoklar().silinenAnahtarlar.contains("s4:42"));
    }

    /** Olmayan cache adı sessizce yutulur (istek düşmez). */
    @Test
    void bilinmeyenCacheAdiHataFirlatmaz() {
        TransactionSynchronizationManager.initSynchronization();
        cacheYardimci.commitSonrasiTemizle("boyleBirCacheYok");
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCommit());
        // Istek dustu; istisna yok.
        assertTrue(true);
    }

    /**
     * Tenant izolasyonu: iki şirket aynı fiziksel cache'i paylaşmamalı.
     * Aksi halde bir şirketin {@code @CacheEvict(allEntries = true)} çağrısı diğer
     * şirketin cache'ini düşürüyordu.
     */
    @Test
    void tenantCacheFizikselAdlariAyirdirir() {
        TenantChecker tc = mock(TenantChecker.class);
        TenantCacheManager tcm = new TenantCacheManager(tc, temelYonlendirici);

        when(tc.getCurrentSirketId()).thenReturn(4L);
        Cache s4Lookup = tcm.getCache("lookup");
        Cache s4Dashboard = tcm.getCache("dashboard");

        when(tc.getCurrentSirketId()).thenReturn(9L);
        Cache s9Lookup = tcm.getCache("lookup");
        Cache s9Dashboard = tcm.getCache("dashboard");

        assertEquals("lookup:t4", s4Lookup.getName());
        assertEquals("lookup:t9", s9Lookup.getName());
        assertEquals("dashboard:t4", s4Dashboard.getName());
        assertEquals("dashboard:t9", s9Dashboard.getName());
        assertNotSame(s4Lookup, s9Lookup);
        assertNotSame(s4Dashboard, s9Dashboard);
    }

    /** Aynı tenant aynı cache örneğini alır (instance caching). */
    @Test
    void ayniTenantAyniCacheOrneginiAlir() {
        TenantChecker tc = mock(TenantChecker.class);
        TenantCacheManager tcm = new TenantCacheManager(tc, temelYonlendirici);
        when(tc.getCurrentSirketId()).thenReturn(4L);

        assertSame(tcm.getCache("lookup"), tcm.getCache("lookup"));
    }

    /** Tenant kapsamlı olmayan cache'ler paylaşılmaya devam eder (dovizKurlari). */
    @Test
    void tenantKapsamsizCachePaylasilir() {
        TenantChecker tc = mock(TenantChecker.class);
        TenantCacheManager tcm = new TenantCacheManager(tc, temelYonlendirici);
        when(tc.getCurrentSirketId()).thenReturn(4L);
        Cache a = tcm.getCache("dovizKurlari");
        when(tc.getCurrentSirketId()).thenReturn(9L);
        Cache b = tcm.getCache("dovizKurlari");

        assertEquals("dovizKurlari", a.getName());
        assertSame(a, b, "Global cache'ler tenant'a bölünmemeli");
    }

    /** Tenant bağlamı yoksa şemasız (paylaşılan) ad kullanılır. */
    @Test
    void tenantBaglamiYoksaPaylasilanAdKullanilir() {
        TenantChecker tc = mock(TenantChecker.class);
        TenantCacheManager tcm = new TenantCacheManager(tc, temelYonlendirici);
        when(tc.getCurrentSirketId()).thenReturn(null);

        Cache c = tcm.getCache("lookup");

        assertEquals("lookup", c.getName());
    }

    /** Bir tenant temizlendiğinde diğer tenant'ın verisi KALIR. */
    @Test
    void temizlemeDigerTenantVerisiniSilmez() {
        TenantChecker tc = mock(TenantChecker.class);
        TenantCacheManager tcm = new TenantCacheManager(tc, temelYonlendirici);

        when(tc.getCurrentSirketId()).thenReturn(4L);
        Cache s4 = tcm.getCache("lookup");
        when(tc.getCurrentSirketId()).thenReturn(9L);
        Cache s9 = tcm.getCache("lookup");

        s4.put("a", "s4-veri");
        s9.put("a", "s9-veri");

        s4.clear();

assertNull(s4.get("a", String.class), "Kendi cache'i temizlenmeli");
        assertEquals("s9-veri", s9.get("a", String.class), "Baske sirketin verisi KORUNMALI");
    }
}