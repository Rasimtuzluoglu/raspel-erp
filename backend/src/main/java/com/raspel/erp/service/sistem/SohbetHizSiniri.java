package com.raspel.erp.service.sistem;

import com.raspel.erp.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Sohbet ve AI uclari icin basit kayan pencere hiz siniri.
 *
 * NEDEN: sohbet uclarinda hicbir hiz siniri yoktu; tek bir kullanici saniyede
 * yuzlerce mesaj/AI sorgusu gonderebiliyordu. Login icin ayri bir filtre vardi
 * ama oturum acmis kullanici icin koruma yoktu.
 *
 * Uygulama bellek icidir (tek instance). Cok instance'li kurulumda Redis
 * tabanli bir sayaçla degistirilebilir; API ayni kalir.
 */
@Component
public class SohbetHizSiniri {

    private final Map<String, Deque<Long>> kayitlar = new ConcurrentHashMap<>();
    private final LongSupplier saat;

    public SohbetHizSiniri() {
        this(System::currentTimeMillis);
    }

    /** Testlerde saati sabitlemek icin. */
    SohbetHizSiniri(LongSupplier saat) {
        this.saat = saat;
    }

    /**
     * @param anahtar  kullanici/eylem bazli anahtar (orn. "mesaj:42")
     * @param adet     pencere icinde izin verilen istek sayisi
     * @param pencereMs pencere uzunlugu (ms)
     * @param mesaj    sinir asilinca firlatilacak kullanici mesaji
     */
    public void kontrolEt(String anahtar, int adet, long pencereMs, String mesaj) {
        long simdi = saat.getAsLong();
        Deque<Long> kuyruk = kayitlar.computeIfAbsent(anahtar, k -> new ArrayDeque<>());
        synchronized (kuyruk) {
            while (!kuyruk.isEmpty() && simdi - kuyruk.peekFirst() >= pencereMs) {
                kuyruk.pollFirst();
            }
            if (kuyruk.size() >= adet) {
                throw new BusinessException(mesaj);
            }
            kuyruk.addLast(simdi);
        }
        // Bellek sismesini engelle: eskimis anahtarlar ara ara temizlenir.
        if (kayitlar.size() > 5000) {
            kayitlar.entrySet().removeIf(e -> {
                Deque<Long> q = e.getValue();
                synchronized (q) {
                    return q.isEmpty() || simdi - q.peekLast() >= pencereMs;
                }
            });
        }
    }
}
