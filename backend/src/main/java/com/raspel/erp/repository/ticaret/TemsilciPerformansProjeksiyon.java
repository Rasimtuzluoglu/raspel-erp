package com.raspel.erp.repository.ticaret;

import java.math.BigDecimal;

/** Temsilci bazlı satış toplamı (performans raporu). */
public interface TemsilciPerformansProjeksiyon {
    Long getTemsilciId();

    String getTemsilciAd();

    Long getFaturaSayisi();

    BigDecimal getToplamSatis();
}
