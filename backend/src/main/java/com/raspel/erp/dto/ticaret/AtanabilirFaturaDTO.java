package com.raspel.erp.dto.ticaret;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Sofor atanabilir fatura ozeti (Faturalar "Sofor Ata" aramasi icin hafif DTO).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtanabilirFaturaDTO {
    private Long id;
    private String faturaNumarasi;
    private LocalDate tarih;
    private Long cariHesapId;
    private String cariHesapAd;
    private String cariAdres;
    private BigDecimal genelToplam;
    private String teslimEden;
}
