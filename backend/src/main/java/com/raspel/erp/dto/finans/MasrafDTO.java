package com.raspel.erp.dto.finans;

import lombok.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasrafDTO {
    private Long id;
    @NotNull(message = "Tarih zorunludur")
    private LocalDate tarih;
    @NotNull(message = "Tutar zorunludur")
    @DecimalMin(value = "0", message = "Tutar negatif olamaz")
    private BigDecimal tutar;
    private String aciklama;
    private String kategori;
    private Long cariHesapId;
    private String belgeNo;
    /** KDV oranı (%); tutar KDV dahil kabul edilir. */
    private BigDecimal kdvOrani;
    private BigDecimal kdvTutar;
    private BigDecimal matrah;
    private String odemeYontemi;
    /** Ödeme yapılan kasa; seçilirse masraf anında kasa çıkışı işlenir. */
    private Long kasaId;
    /** Ödeme yapılan banka; seçilirse masraf anında banka çıkışı işlenir. */
    private Long bankaId;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}
