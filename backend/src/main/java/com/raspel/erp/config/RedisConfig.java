package com.raspel.erp.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig implements org.springframework.cache.annotation.CachingConfigurer {

    private static GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Guvenlik: yalnizca bilinen paketlere polimorfik tip cozumlemesine izin ver
        // (LaissezFaire yerine allowlist) -> deserialization gadget yuzeyi kapatilir.
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.raspel.erp.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.math.")
                .allowIfSubType("java.lang.")
                .build();
        mapper.activateDefaultTyping(
                ptv,
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
            RedisCacheConfiguration defaultCacheConfig,
            org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory) {
        return builder -> builder
                // KEYS komutu kapalı Redis'te temizliğin SCAN ile yapılmasını sağlar.
                .cacheWriter(new ScanRedisCacheWriter(connectionFactory))
                .cacheDefaults(defaultCacheConfig)
                .withCacheConfiguration("dashboard", defaultCacheConfig.entryTtl(Duration.ofMinutes(2)))
                .withCacheConfiguration("cariHesaplar", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("faturalar", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("stoklar", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("lookup", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
    }

    /**
     * Redis cache yöneticisi: tenant izole adlandırma sarmalayıcısıyla birlikte.
     *
     * <p><b>Neden elle kuruluyor?</b> {@code @Primary} bir {@code CacheManager}
     * tanımlamak Spring Boot'un otomatik yapılandırmasını devre dışı bırakır;
     * otomatik yönetici bir bean OLARAK var olmadığı için onu
     * {@code ObjectProvider} ile almak dairesel bağımlılık üretir. Bu yüzden
     * temel yönetici doğrudan kurulur, sonra {@link TenantCacheManager} ile
     * sarılır. Böylece:
     * <ul>
     *   <li>{@code lookup} ve {@code dashboard} fiziksel adları
     *       {@code <ad>:t<sirketId>} olur; {@code @CacheEvict(allEntries = true)}
     *       yalnızca o şirketin verisini siler.</li>
     *   <li>Redis yoksa (test/dev profili) bellek içi cache'e düşülür; uygulama
     *       ayaga kalkmaya devam eder.</li>
     * </ul>
     */
    @Bean
    @org.springframework.context.annotation.Primary
    public CacheManager cacheManager(TenantChecker tenantChecker,
                                      org.springframework.beans.factory.ObjectProvider<
                                              org.springframework.data.redis.connection.RedisConnectionFactory> connectionFactory,
                                      RedisCacheConfiguration defaultCacheConfig) {
        var cf = connectionFactory.getIfAvailable();
        if (cf == null) {
            log.warn("Redis baglantisi yok; bellek ici cache kullanilacak (tenant izole adlandirma aktif).");
            return new TenantCacheManager(tenantChecker,
                    new org.springframework.cache.concurrent.ConcurrentMapCacheManager());
        }
        RedisCacheManager temel = RedisCacheManager.builder(cf)
                .cacheWriter(new ScanRedisCacheWriter(cf))
                .cacheDefaults(defaultCacheConfig)
                // Statik TTL'ler cacheManagerCustomizer ile ayni degerleri kullanir.
                .withCacheConfiguration("dashboard", defaultCacheConfig.entryTtl(Duration.ofMinutes(2)))
                .withCacheConfiguration("cariHesaplar", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("faturalar", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("stoklar", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("lookup", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)))
                .transactionAware()
                .build();
        return new TenantCacheManager(tenantChecker, temel);
    }

    /**
     * Cache hatalarının isteği düşürmemesi için error handler'ın Spring cache
     * altyapısına kaydedilmesi gerekir (düz @Bean olarak tanımlamak yetmez).
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return cacheErrorHandler();
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