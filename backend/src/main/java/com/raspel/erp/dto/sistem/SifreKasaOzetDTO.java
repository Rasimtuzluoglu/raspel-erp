package com.raspel.erp.dto.sistem;

import lombok.*;

/**
 * Sidebar rozeti ve kasa ekrani ust kismi icin ozet sayaclar.
 *
 * <p>Onay sayaci ucu ({@code /api/onay-sayilari}) ile ayni deseni izler:
 * istemci ucun uzerinde hesaplar, sunucu yalniz COUNT doner.
 * Arsivlenen kayitlar sure sayaçlarina GIRMEZ.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SifreKasaOzetDTO {

    /** 14 gun veya daha az kalan (SURE_YAKLASTI + SURESI_BITTI). */
    private Long uyari;

    /** Suresi dolmus kayit sayisi. */
    private Long suresiBitti;

    /** Aktif + GLOBAL kapsamda, gorunurlugu TUMU olan kayit sayisi. */
    private Long sirketGeneli;

    /** Arsivlenen kayit sayisi. */
    private Long arsiv;
}