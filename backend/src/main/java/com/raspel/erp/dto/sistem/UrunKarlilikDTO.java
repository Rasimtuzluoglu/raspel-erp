package com.raspel.erp.dto.sistem;

import lombok.*;
import java.math.BigDecimal;

/**
 * Ürün bazlı kârlılık raporu satırı: alış maliyeti (fiyat) ile satış fiyatı karşılaştırması.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrunKarlilikDTO {
    private Long stokId;
    private String stokKodu;
    private String stokAd;
    private BigDecimal alisFiyat;
    private BigDecimal satisFiyati;
    private BigDecimal kar;
    private BigDecimal karMarji;
}
