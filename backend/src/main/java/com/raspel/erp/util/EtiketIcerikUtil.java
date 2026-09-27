package com.raspel.erp.util;

import com.raspel.erp.entity.envanter.Stok;

/**
 * Raf etiketi QR/barkod icerigi icin tek kaynak. Onizleme (frontend) ve PDF
 * uretimi ayni kurali kullanir: barkod -> stok kodu -> STK{id}.
 *
 * CODE128 yalnizca ASCII destekler; Turkce karakterler kayipsiz sekilde
 * translitere edilir (İ->I, Ş->S, Ğ->G, Ü->U, Ö->O, Ç->C), kalan ASCII disi
 * karakterler atilir. Boylece onizlemede cizilen cubuklar ile PDF'te basilan
 * cubuklar ayni degeri kodlar.
 */
public final class EtiketIcerikUtil {

    private EtiketIcerikUtil() {
    }

    /** Insan okur etiket kodu: barkod varsa barkod, yoksa stok kodu, o da yoksa STK{id}. */
    public static String gosterim(Stok stok) {
        if (stok == null) return "";
        if (stok.getBarkod() != null && !stok.getBarkod().isBlank()) return stok.getBarkod().trim();
        if (stok.getStokKodu() != null && !stok.getStokKodu().isBlank()) return stok.getStokKodu().trim();
        return stok.getId() != null ? "STK" + stok.getId() : "";
    }

    /** QR icerigi: taraninca urunun bulunabilmesi icin etiket kodu kodlanir. */
    public static String qrIcerik(Stok stok) {
        return gosterim(stok);
    }

    /** CODE128 icerigi: ASCII'ye sadelestirilmis etiket kodu. */
    public static String barkodIcerik(Stok stok) {
        String sade = asciiSadelestir(gosterim(stok));
        if (!sade.isBlank()) return sade;
        return stok != null && stok.getId() != null ? "STK" + stok.getId() : "STK";
    }

    public static String asciiSadelestir(String ham) {
        if (ham == null) return "";
        StringBuilder sb = new StringBuilder(ham.length());
        for (char c : ham.toCharArray()) {
            switch (c) {
                case 'İ' -> sb.append('I');
                case 'ı' -> sb.append('i');
                case 'Ş' -> sb.append('S');
                case 'ş' -> sb.append('s');
                case 'Ğ' -> sb.append('G');
                case 'ğ' -> sb.append('g');
                case 'Ü' -> sb.append('U');
                case 'ü' -> sb.append('u');
                case 'Ö' -> sb.append('O');
                case 'ö' -> sb.append('o');
                case 'Ç' -> sb.append('C');
                case 'ç' -> sb.append('c');
                default -> {
                    if (c >= 0x20 && c <= 0x7E) sb.append(c);
                }
            }
        }
        return sb.toString().trim();
    }
}
