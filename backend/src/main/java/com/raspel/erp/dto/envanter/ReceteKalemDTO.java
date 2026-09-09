package com.raspel.erp.dto.envanter;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceteKalemDTO {
    private Long id;
    private Long receteId;
    private Long hammaddeId;
    private String hammaddeAd;
    private BigDecimal miktar;
}
