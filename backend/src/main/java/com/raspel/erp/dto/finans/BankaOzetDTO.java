package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;

/**
 * Bankalar ekranı KPI özeti. Sayaç/tutarlar şirket kapsamındadır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankaOzetDTO {
    private long toplamBanka;
    private BigDecimal toplamBakiye;
    private BigDecimal pozitifBakiye;
    private long eslesmemisHareket;
}
