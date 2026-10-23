package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaksitPlanOzetDTO {
    private String planNo;
    private String cariAd;
    private String kurum;
    private long taksitSayisi;
    private long odenenAdet;
    private BigDecimal toplamTutar;
    private BigDecimal odenenTutar;
    private BigDecimal kalanTutar;
    private LocalDate ilkVade;
    private LocalDate sonVade;
}
