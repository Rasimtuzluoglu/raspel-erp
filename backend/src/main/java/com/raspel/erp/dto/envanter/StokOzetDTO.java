package com.raspel.erp.dto.envanter;

import lombok.*;

import java.math.BigDecimal;

/**
 * Stok liste ekrani ust ozet (KPI) verileri.
 *
 * <p>Tek sorgu setiyle (count + kritik count + stok degeri) hesaplanir; tum
 * stoklarin entity olarak yuklenmesini gerektirmez.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokOzetDTO {

    /** Sirkete ait toplam stok kaydi. */
    private long toplamUrun;

    /** Kritik seviyeye dusen stok adedi (miktar <= minMiktar). */
    private long kritikSayi;

    /** Toplam stok degeri (Σ miktar x alis fiyati). */
    private BigDecimal toplamDeger;

    /** Farkli kategori sayisi. */
    private long kategoriSayisi;
}
