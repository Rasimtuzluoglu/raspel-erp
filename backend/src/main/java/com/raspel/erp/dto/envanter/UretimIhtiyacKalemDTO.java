package com.raspel.erp.dto.envanter;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimIhtiyacKalemDTO {
    private Long hammaddeId;
    private String hammaddeAd;
    private String birim;
    private BigDecimal gerekli;
    private BigDecimal mevcut;
    private BigDecimal eksik;
    private BigDecimal birimFiyat;
    private BigDecimal tutar;
}
