package com.raspel.erp.util;

import com.raspel.erp.exception.BusinessException;

/**
 * Sohbet mesajlarinda paylasilan dosya baglantisi dogrulamasi.
 *
 * NEDEN: `dosyaUrl` istemciden geldigi gibi kaydediliyordu. Bir kullanici
 * baska bir siteye ait gorsel URL'i gonderebiliyor, oda uyelerinin tarayicisi
 * bu adrese istek atarak IP/bilgi sizintisina yol acabiliyordu. Ayrica
 * `//evil.com` gibi protokol-goreli adresler guvenli URL kontrolunden
 * geciyordu. Burada yalnizca kendi yukleme yolumuz kabul edilir.
 */
public final class SohbetDosyaUrl {

    /** Yukleme uclarinin dondurdugu tek onek. */
    public static final String ONEK = "/api/uploads/sohbet/";

    private SohbetDosyaUrl() {
    }

    /**
     * Bos/null URL kabul edilir (dosyasiz mesaj). Dolu ise tek segmentli,
     * kendi yukleme onegimizle baslayan bir yol olmalidir.
     *
     * @throws BusinessException gecersiz baglanti
     */
    public static void dogrula(String url) {
        if (url == null || url.isBlank()) return;
        String u = url.trim();
        if (!u.startsWith(ONEK)) {
            throw new BusinessException("Geçersiz dosya bağlantısı");
        }
        String dosyaAdi = u.substring(ONEK.length());
        if (dosyaAdi.isBlank()
                || dosyaAdi.contains("/")
                || dosyaAdi.contains("\\")
                || dosyaAdi.contains("..")
                || dosyaAdi.contains("?")
                || dosyaAdi.contains("#")) {
            throw new BusinessException("Geçersiz dosya bağlantısı");
        }
    }
}
