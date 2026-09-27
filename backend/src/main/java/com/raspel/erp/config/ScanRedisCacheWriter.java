package com.raspel.erp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Redis cache temizliğini KEYS yerine SCAN + DEL ile yapan cache writer.
 *
 * <p>Üretim Redis'inde {@code KEYS} komutu güvenlik nedeniyle kapatılmıştır
 * ({@code rename-command KEYS ""}); varsayılan yazıcı {@code clean()}
 * çağrısında KEYS kullandığı için {@code @CacheEvict(allEntries = true)}
 * işlemleri (dolayısıyla tüm güncelleme istekleri) 5xx veriyordu. SCAN bloklamadan
 * ilerler; anahtarlar partiler halinde silinir. Diğer tüm işlemler varsayılan
 * yazıcıya devredilir.
 */
@Slf4j
public class ScanRedisCacheWriter implements RedisCacheWriter {

    private static final int TARAMA_ADIMI = 1000;

    private final RedisCacheWriter delegate;
    private final RedisConnectionFactory connectionFactory;

    public ScanRedisCacheWriter(RedisConnectionFactory connectionFactory) {
        this.delegate = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory);
        this.connectionFactory = connectionFactory;
    }

    @Override
    public byte[] get(String name, byte[] key) {
        return delegate.get(name, key);
    }

    @Override
    public CompletableFuture<byte[]> retrieve(String name, byte[] key, Duration ttl) {
        return delegate.retrieve(name, key, ttl);
    }

    @Override
    public void put(String name, byte[] key, byte[] value, Duration ttl) {
        delegate.put(name, key, value, ttl);
    }

    @Override
    public CompletableFuture<Void> store(String name, byte[] key, byte[] value, Duration ttl) {
        return delegate.store(name, key, value, ttl);
    }

    @Override
    public byte[] putIfAbsent(String name, byte[] key, byte[] value, Duration ttl) {
        return delegate.putIfAbsent(name, key, value, ttl);
    }

    @Override
    public void remove(String name, byte[] key) {
        delegate.remove(name, key);
    }

    @Override
    public void clean(String name, byte[] pattern) {
        String match = pattern != null && pattern.length > 0
                ? new String(pattern, StandardCharsets.UTF_8)
                : name + "::*";
        tarayarakSil(match);
    }

    @Override
    public void clearStatistics(String name) {
        delegate.clearStatistics(name);
    }

    @Override
    public RedisCacheWriter withStatisticsCollector(CacheStatisticsCollector cacheStatisticsCollector) {
        delegate.withStatisticsCollector(cacheStatisticsCollector);
        return this;
    }

    @Override
    public CacheStatistics getCacheStatistics(String cacheName) {
        return delegate.getCacheStatistics(cacheName);
    }

    private void tarayarakSil(String match) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            ScanOptions options = ScanOptions.scanOptions().match(match).count(TARAMA_ADIMI).build();
            List<byte[]> parti = new ArrayList<>(TARAMA_ADIMI);
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    parti.add(cursor.next());
                    if (parti.size() >= TARAMA_ADIMI) {
                        connection.del(parti.toArray(new byte[0][]));
                        parti.clear();
                    }
                }
            }
            if (!parti.isEmpty()) {
                connection.del(parti.toArray(new byte[0][]));
            }
        } catch (Exception e) {
            // Cache temizliği başarısız olsa da iş akışı durmaz (TTL ile kendini yeniler).
            log.warn("Cache SCAN ile temizlenemedi [{}]: {}", match, e.getMessage());
        }
    }
}
