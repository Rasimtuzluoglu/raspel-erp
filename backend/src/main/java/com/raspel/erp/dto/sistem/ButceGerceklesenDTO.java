package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ButceGerceklesenDTO {
    private String kategori;
    private BigDecimal butce;
    private BigDecimal gerceklesen;
    private BigDecimal sapma;
    private BigDecimal kullanimYuzdesi;
}
