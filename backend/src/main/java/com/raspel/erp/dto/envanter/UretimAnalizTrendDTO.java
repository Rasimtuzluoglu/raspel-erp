package com.raspel.erp.dto.envanter;

import lombok.*;

import java.math.BigDecimal;

/** Aylık üretim/fire/maliyet seyri. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimAnalizTrendDTO {
    /** YYYY-MM */
    private String donem;
    private BigDecimal uretilen;
    private BigDecimal fire;
    private BigDecimal maliyet;
}
