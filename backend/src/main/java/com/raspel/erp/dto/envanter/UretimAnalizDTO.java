package com.raspel.erp.dto.envanter;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** Yönetim odaklı üretim analizi: dönem özeti, ürün kırılımı ve aylık seyir. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimAnalizDTO {
    private BigDecimal toplamUretilen;
    private BigDecimal toplamFire;
    private BigDecimal fireOrani;
    private BigDecimal toplamMaliyet;
    private BigDecimal birimMaliyet;
    private BigDecimal ortalamaSureSaat;
    private long tamamlananEmir;
    private List<UretimAnalizUrunDTO> urunler;
    private List<UretimAnalizTrendDTO> trend;
}
