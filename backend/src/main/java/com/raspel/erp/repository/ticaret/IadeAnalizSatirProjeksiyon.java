package com.raspel.erp.repository.ticaret;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ürün analizi için tamamlanmış iade kalemlerinin düz satır projeksiyonu.
 * Cari bilgisi bağlantılı faturadan gelir (iade tablosunda cari yoktur).
 */
public interface IadeAnalizSatirProjeksiyon {
    Long getIadeId();
    LocalDate getIadeTarihi();
    String getIadeTuru();
    Long getCariHesapId();
    String getCariHesapAd();
    BigDecimal getMiktar();
    BigDecimal getBirimFiyat();
}