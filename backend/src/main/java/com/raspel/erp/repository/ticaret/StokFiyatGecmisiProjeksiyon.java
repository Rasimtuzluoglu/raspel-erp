package com.raspel.erp.repository.ticaret;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bir stogun gecmis alis fiyatlarinin projeksiyonu.
 */
public interface StokFiyatGecmisiProjeksiyon {
    BigDecimal getBirimFiyat();
    LocalDate getTarih();
    String getFaturaNumarasi();
}
