package com.raspel.erp.dto.envanter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AylikFiyatDTO {
    private int yil;
    private int ay;
    private BigDecimal ortalamaAlisFiyati;
    private BigDecimal ortalamaSatisFiyati;
    private BigDecimal toplamAlisMiktar;
    private BigDecimal toplamSatisMiktar;
}