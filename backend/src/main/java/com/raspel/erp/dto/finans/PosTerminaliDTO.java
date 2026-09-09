package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosTerminaliDTO {
    private Long id;
    private Long sirketId;
    private String ad;
    private Long bankaId;
    private String bankaAd;
    private BigDecimal komisyonOrani;
    private Boolean aktif;
}
