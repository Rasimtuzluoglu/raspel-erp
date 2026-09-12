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
public class TedarikciAnalizDTO {
    private Long cariHesapId;
    private String cariHesapAd;
    private BigDecimal toplamMiktar;
    private BigDecimal toplamTutar;
    private BigDecimal ortalamaFiyat;
    private BigDecimal sonFiyat;
    private LocalDate sonAlisTarihi;
    private int islemSayisi;
}