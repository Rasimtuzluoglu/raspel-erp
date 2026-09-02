package com.raspel.erp.repository.ticaret;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bir cari hesabın belirli bir ürünü geçmişte aldığı fiyatların projeksiyonu.
 * (cari + stok bazında satış fiyat geçmişi)
 */
public interface CariUrunFiyatProjeksiyon {
    BigDecimal getBirimFiyat();
    LocalDate getTarih();
    String getFaturaNumarasi();
    Integer getAdet();
}
