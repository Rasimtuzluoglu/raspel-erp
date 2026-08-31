package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnayAyariDTO {
    private Long id;
    private Long sirketId;
    private String modul;
    private BigDecimal esikTutar;
    private Boolean otomatikOnay;
}
