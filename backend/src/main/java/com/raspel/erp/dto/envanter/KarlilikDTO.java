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
public class KarlilikDTO {
    private Long stokId;
    private String stokAd;
    private String stokKodu;
    private BigDecimal stokMiktar;
    private BigDecimal ortalamaMaliyet;
    private BigDecimal stokMaliyeti;
    private BigDecimal ortalamaSatisFiyati;
    private BigDecimal birimBrutKar;
    private BigDecimal toplamBrutKar;
    private BigDecimal brutKarMarji;
    private BigDecimal satisIadeTutari;
    private int satilanMiktar;
}