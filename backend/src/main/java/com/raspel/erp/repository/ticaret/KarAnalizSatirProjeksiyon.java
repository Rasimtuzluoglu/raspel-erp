package com.raspel.erp.repository.ticaret;

import java.math.BigDecimal;

/**
 * Kâr analizi kırılım satırı (ürün veya cari).
 *
 * <p>Her iki sorgu da grubun kimliğini {@code id} takma adıyla döner; ürün
 * sorgusunda {@code id = stokId}, cari sorgusunda {@code id = cariHesap.id}.
 */
public interface KarAnalizSatirProjeksiyon {
    Long getId();
    String getAd();
    BigDecimal getCiro();
    BigDecimal getMaliyet();
}
