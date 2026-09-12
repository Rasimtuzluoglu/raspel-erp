package com.raspel.erp.repository.ticaret;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ürün analizi için kesilmiş fatura kalemlerinin düz satır projeksiyonu.
 * (tur ALIS/SATIS, durum KESILDI faturalar; cari_hesap null olabilir.)
 */
public interface FaturaAnalizSatirProjeksiyon {
    LocalDate getFaturaTarihi();
    String getFaturaNumarasi();
    Long getCariHesapId();
    String getCariHesapAd();
    Integer getAdet();
    BigDecimal getBirimFiyat();
    BigDecimal getIskontoOrani();
}