package com.raspel.erp.repository.ticaret;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tedarikçi bazlı ürün raporu projeksiyonu.
 * Alış (ALIS + KESILDI) faturalarından tedarikçi -> ürün toplamlarını taşır.
 */
public interface TedarikciUrunProjeksiyon {
    Long getCariHesapId();
    String getCariHesapAd();
    Long getStokId();
    Long getToplamMiktar();
    BigDecimal getSonBirimFiyat();
    LocalDate getSonTarih();
}
