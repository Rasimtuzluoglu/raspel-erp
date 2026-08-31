package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChurnRiskDTO {
    private Long cariId;
    private String cariAd;
    private String seviye; // YUKSEK, ORTA, DUSUK
    private int skor; // 0-100
    private Integer sonIslemGunOnce;
    private Long toplamIslemSayisi;
    private BigDecimal toplamCiro;
    private String oneri;
}
