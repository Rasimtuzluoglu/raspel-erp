package com.raspel.erp.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

    private static GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisCacheConfiguration defaultCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer()));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheManagerCustomizer(
            RedisCacheConfiguration defaultCacheConfig) {
        return builder -> builder
                .withCacheConfiguration("dashboard", defaultCacheConfig.entryTtl(Duration.ofMinutes(2)))
                .withCacheConfiguration("cariHesaplar", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("faturalar", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("stoklar", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("lookup", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
    }

    /**
     * Redis erisilemezken cache islemleri basarisiz oldugunda uygulamanin
     * veritabanindan okumaya devam etmesini saglar. Cache gecici olarak
     * devre disi kalir, istekler 5xx almaz.
     */
    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache okuma hatasi (cache atlaniyor): cache={}, key={}, hata={}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Cache yazma hatasi (cache atlaniyor): cache={}, key={}, hata={}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache evict hatasi: cache={}, key={}, hata={}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Cache temizleme hatasi: cache={}, hata={}",
                        cache.getName(), exception.getMessage());
            }
        };
    }
}