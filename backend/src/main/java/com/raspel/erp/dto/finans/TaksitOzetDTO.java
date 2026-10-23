package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaksitOzetDTO {
    private BigDecimal bekleyenToplam;
    private long bekleyenAdet;
    private BigDecimal gecikmisToplam;
    private long gecikmisAdet;
    private BigDecimal buAyToplam;
    private BigDecimal odenenBuYilToplam;
    private long odenenBuYilAdet;
}
