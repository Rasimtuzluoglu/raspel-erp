package com.raspel.erp.dto.sistem;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tedarikçi bazlı ürün raporu satırı.
 * Hangi tedarikçiden hangi ürünün, toplam miktar ve son fiyat bilgisiyle geldiğini taşır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TedarikciUrunDTO {
    private Long cariHesapId;
    private String cariHesapAd;
    private Long stokId;
    private String stokAd;
    private String stokKodu;
    private Long toplamMiktar;
    private BigDecimal sonBirimFiyat;
    private LocalDate sonTarih;
}
