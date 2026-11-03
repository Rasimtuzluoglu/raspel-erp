package com.raspel.erp.repository.ticaret;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Vade takvimi için minimal fatura projeksiyonu.
 *
 * <p>Yalnızca gerekli altı kolon okunur; {@code Fatura} entity'si (kalemler ve
 * cari ilişkisi) belleğe yüklenmez.
 */
public interface VadeTakvimProjeksiyon {

    Long getFaturaId();

    String getFaturaNumarasi();

    String getCariAd();

    LocalDate getVadeTarihi();

    BigDecimal getGenelToplam();

    BigDecimal getOdenenTutar();
}
