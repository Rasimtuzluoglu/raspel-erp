package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;

/**
 * Kasa ekranı KPI özeti. Tutarlar şirket kapsamındadır; gelir/gider
 * <b>bugünkü</b> kasa hareketlerini ifade eder.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KasaOzetDTO {
    private long toplamKasa;
    private BigDecimal toplamBakiye;
    private BigDecimal bugunGelir;
    private BigDecimal bugunGider;
}
