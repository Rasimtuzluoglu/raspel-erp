package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;

/**
 * Faz 2.1: Kredi limiti risk özeti. Bir cari, tanımlı kredi limitini aştığında
 * bu DTO ile listelenir (borç, limit, aşım tutarı ve risk oranı).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariRiskDTO {

    private Long cariId;
    private String cariAd;
    private String telefon;
    private String email;
    private String temsilciAd;

    /** Cari bakiye (negatif = cari bize borçlu). */
    private BigDecimal bakiye;
    /** Borç tutarı (negatif bakiyenin mutlak değeri). */
    private BigDecimal borc;
    private BigDecimal krediLimiti;
    /** Borç - limit (pozitif = aşım). */
    private BigDecimal asimTutari;
    /** Borç / limit * 100. */
    private BigDecimal riskOrani;
}
