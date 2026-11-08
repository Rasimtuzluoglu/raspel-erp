package com.raspel.erp.repository.ticaret;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Kokpit nakit takvimi için gün bazlı vade agregasyonu. */
public interface VadeGunProjeksiyon {

    LocalDate getTarih();

    Long getAdet();

    BigDecimal getTutar();
}
