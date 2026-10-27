package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;

/** Bütçe–gerçekleşen raporu tek satırı. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ButceGerceklesenSatirDTO {
    private String kategori;
    private BigDecimal butce;
    /** Gelir raporunda kategori kırılımı yoksa null olabilir. */
    private BigDecimal gerceklesen;
    private BigDecimal sapma;
    private BigDecimal kullanimYuzdesi;
}
