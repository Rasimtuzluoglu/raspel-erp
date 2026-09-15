package com.raspel.erp.repository.ticaret;

import java.math.BigDecimal;

/**
 * Bir cari+stok icin en son (tarihe gore) satis fiyatini tasiyan projeksiyon.
 * MAX(birimFiyat) yerine gercek "son" fiyati verir.
 */
public interface CariUrunSonFiyatProjeksiyon {
    Long getStokId();
    BigDecimal getBirimFiyat();
}
