package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;

/** Bütçe ekranı KPI özeti (yıl + tür bazlı). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ButceOzetDTO {
    private BigDecimal butceToplam;
    private BigDecimal gerceklesenToplam;
    /** gerçekleşen - bütçe; pozitif değer aşım demektir. */
    private BigDecimal sapma;
    /** gerçekleşen / bütçe * 100; bütçe yoksa null. */
    private BigDecimal kullanimYuzde;
    private BigDecimal gelirButce;
    private BigDecimal giderButce;
    private BigDecimal gelirGerceklesen;
    private BigDecimal giderGerceklesen;
    private long kayitSayisi;
    private long kategoriSayisi;
    /** Onay bekleyen (TASLAK) bütçe sayısı. */
    private long onayBekleyen;
}
