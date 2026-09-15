package com.raspel.erp.dto.envanter;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimOzetDTO {
    private long taslak;
    private long uretimde;
    private long tamamlandi;
    private long iptal;
    private long geciken;
    private long buAyTamamlanan;
    private BigDecimal fireOrani;
    private BigDecimal ortalamaSureSaat;
}
