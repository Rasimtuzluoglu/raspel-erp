package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Birleşik onay listesindeki tek bir bekleyen kayıt.
 *
 * <p>Tüm türler aynı alanlarla gösterilir; türe özel ek bilgiler
 * {@link #meta} haritasında taşınır (ör. izin bitiş tarihi, avans mı).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnayBekleyenDTO {
    private String tur;
    private Long kaynakId;
    private String baslik;
    private String altBaslik;
    private BigDecimal tutar;
    private LocalDate tarih;
    private LocalDateTime olusturmaTarihi;
    private String durum;
    private String aciklama;
    private Map<String, Object> meta;
}
