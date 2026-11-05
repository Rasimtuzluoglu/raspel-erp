package com.raspel.erp.repository.ticaret;

import java.math.BigDecimal;

/**
 * Kâr analizi dönem özeti: ciro ve maliyet toplamları.
 *
 * <p>Kayıt yoksa toplamlar {@code null} döner; servis sıfıra normalize eder.
 */
public interface KarAnalizOzetProjeksiyon {
    BigDecimal getCiro();
    BigDecimal getMaliyet();
}
