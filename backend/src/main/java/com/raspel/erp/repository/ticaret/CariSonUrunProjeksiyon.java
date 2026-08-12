package com.raspel.erp.repository.ticaret;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cari hesabin son aldigi urunlerin projeksiyonu.
 * FaturaKalemRepository.cariSonUrunler sorgusu tarafindan doldurulur.
 */
public interface CariSonUrunProjeksiyon {
    Long getStokId();
    LocalDate getSonAlisTarihi();
    BigDecimal getSonBirimFiyat();
    Long getAdet();
}
