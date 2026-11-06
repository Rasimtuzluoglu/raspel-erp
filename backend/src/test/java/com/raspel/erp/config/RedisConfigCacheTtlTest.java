package com.raspel.erp.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class RedisConfigCacheTtlTest {

    @Test
    void kokpitCacheTtl30Saniyedir() {
        RedisConfig config = new RedisConfig();
        RedisCacheConfiguration defaults = config.defaultCacheConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory);
        config.cacheManagerCustomizer(defaults, connectionFactory).customize(builder);
        RedisCacheManager manager = builder.build();
        manager.afterPropertiesSet();

        RedisCacheConfiguration kokpit = manager.getCacheConfigurations().get("kokpit");
        assertNotNull(kokpit, "kokpit cache konfigürasyonu tanımlı olmalı");
        assertEquals(Duration.ofSeconds(30), kokpit.getTtl());
    }

    @Test
    void redisHatasiIstemeyiBozmaz() {
        CacheErrorHandler handler = new RedisConfig().cacheErrorHandler();
        Cache cache = mock(Cache.class);
        RuntimeException hata = new RuntimeException("redis kapalı");

        assertDoesNotThrow(() -> handler.handleCacheGetError(hata, cache, "anahtar"));
        assertDoesNotThrow(() -> handler.handleCachePutError(hata, cache, "anahtar", "değer"));
        assertDoesNotThrow(() -> handler.handleCacheEvictError(hata, cache, "anahtar"));
        assertDoesNotThrow(() -> handler.handleCacheClearError(hata, cache));
    }
}
