package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosOzetDTO {
    private Long posId;
    private String posAd;
    private String bankaAd;
    private BigDecimal bugunTutar;
    private BigDecimal bugunKomisyon;
    private BigDecimal toplamTutar;
    private BigDecimal toplamKomisyon;
}
