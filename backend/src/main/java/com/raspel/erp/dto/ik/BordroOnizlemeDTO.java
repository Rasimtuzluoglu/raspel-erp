package com.raspel.erp.dto.ik;

import lombok.*;

import java.math.BigDecimal;

/**
 * Toplu bordro üretimi öncesi tek personel satırı önizlemesi.
 * durum: UYGUN | MAAS_YOK | ZATEN_VAR
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BordroOnizlemeDTO {
    private Long personelId;
    private String personelAdi;
    private BigDecimal brutMaas;
    private BigDecimal netMaas;
    private String durum;
}
