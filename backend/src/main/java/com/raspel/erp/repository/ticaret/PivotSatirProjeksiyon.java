package com.raspel.erp.repository.ticaret;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pivot tablo için fatura kalemi düz satır projeksiyonu.
 */
public interface PivotSatirProjeksiyon {
    Long getCariHesapId();
    String getCariAd();
    Long getStokId();
    String getStokAd();
    String getKategori();
    String getTur();
    String getOdemeDurumu();
    LocalDate getTarih();
    BigDecimal getTutar();
    Integer getAdet();
}
