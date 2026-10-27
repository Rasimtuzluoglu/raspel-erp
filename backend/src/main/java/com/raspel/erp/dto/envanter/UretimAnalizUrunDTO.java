package com.raspel.erp.dto.envanter;

import lombok.*;

import java.math.BigDecimal;

/** Ürün bazlı üretim maliyeti ve fire özeti. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimAnalizUrunDTO {
    private Long urunId;
    private String urunAd;
    private long emirSayisi;
    private BigDecimal uretilen;
    private BigDecimal fire;
    private BigDecimal fireOrani;
    private BigDecimal toplamMaliyet;
    private BigDecimal birimMaliyet;
}
