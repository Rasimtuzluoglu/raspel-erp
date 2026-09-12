package com.raspel.erp.dto.envanter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IslemSatirDTO {
    private LocalDate tarih;
    private String belgeNo;
    private String tur;
    private String cariHesapAd;
    private BigDecimal miktar;
    private BigDecimal birimFiyat;
    private BigDecimal tutar;
}