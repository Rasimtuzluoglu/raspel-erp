package com.raspel.erp.service.sistem;

import com.raspel.erp.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Sohbet ve AI uclari icin kayan pencere hiz siniri.
 *
 * Faz 9: Once yalnizca bellek ici sayac vardi; cok instance'li kurulumda her
 * instance kendi limitini uyguluyor, toplamda limitin N kati asilabiliyordu.
 * Artik mumkunse Redis sorted-set ile ORTAK sayac kullanilir (atomik, tum
 * instance'lar ayni pencereyi gorur). Redis yoksa/erisilemezse bellek ici
 * yedege dusulur; boylece Redis kesintisi sohbeti kilitlemez.
 */
@Component
@Slf4j
public class SohbetHizSiniri {

    private static final String ONEK = "sohbet:hiz:";

    private final StringRedisTemplate redis;
    private final LongSupplier saat;
    private final Map<String, Deque<Long>> yerel = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public SohbetHizSiniri(ObjectProvider<StringRedisTemplate> redisSaglayici) {
        this(redisSaglayici.getIfAvailable(), System::currentTimeMillis);
    }

    /** Testlerde saati/redis'i sabitlemek icin. */
    SohbetHizSiniri(StringRedisTemplate redis, LongSupplier saat) {
        this.redis = redis;
        this.saat = saat;
    }

    /**
     * @param anahtar   kullanici/eylem bazli anahtar (orn. "mesaj:42")
     * @param adet      pencere icinde izin verilen istek sayisi
     * @param pencereMs pencere uzunlugu (ms)
     * @param mesaj     sinir asilinca firlatilacak kullanici mesaji
     */
    public void kontrolEt(String anahtar, int adet, long pencereMs, String mesaj) {
        if (redis != null) {
            try {
                redisIleKontrol(anahtar, adet, pencereMs, mesaj);
                return;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Redis hiz siniri kullanilamadi, bellek ici sayaca geciliyor: {}", e.getMessage());
            }
        }
        yerelKontrol(anahtar, adet, pencereMs, mesaj);
    }

    private void redisIleKontrol(String anahtar, int adet, long pencereMs, String mesaj) {
        String k = ONEK + anahtar;
        long simdi = saat.getAsLong();
        var z = redis.opsForZSet();
        z.removeRangeByScore(k, 0, simdi - pencereMs);
        Long boyut = z.zCard(k);
        if (boyut != null && boyut >= adet) {
            throw new BusinessException(mesaj);
        }
        z.add(k, UUID.randomUUID().toString(), simdi);
        redis.expire(k, Duration.ofMillis(pencereMs));
    }

    private void yerelKontrol(String anahtar, int adet, long pencereMs, String mesaj) {
        long simdi = saat.getAsLong();
        Deque<Long> kuyruk = yerel.computeIfAbsent(anahtar, k -> new ArrayDeque<>());
        synchronized (kuyruk) {
            while (!kuyruk.isEmpty() && simdi - kuyruk.peekFirst() >= pencereMs) {
                kuyruk.pollFirst();
            }
            if (kuyruk.size() >= adet) {
                throw new BusinessException(mesaj);
            }
            kuyruk.addLast(simdi);
        }
        if (yerel.size() > 5000) {
            yerel.entrySet().removeIf(e -> {
                Deque<Long> q = e.getValue();
                synchronized (q) {
                    return q.isEmpty() || simdi - q.peekLast() >= pencereMs;
                }
            });
        }
    }
}
