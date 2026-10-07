package com.raspel.erp.dto.finans;

import lombok.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
    @Digits(integer = 17, fraction = 2, message = "Tutar en fazla 2 ondalık basamak olabilir")
    private BigDecimal tutar;
    private String aciklama;
    private String kategori;
    private Long cariHesapId;
    private String belgeNo;
    /** KDV oranı (%); tutar KDV dahil kabul edilir. */
    @DecimalMin(value = "0", message = "KDV oranı negatif olamaz")
    @DecimalMax(value = "100", message = "KDV oranı en fazla 100 olabilir")
    @Digits(integer = 5, fraction = 2, message = "KDV oranı en fazla 2 ondalık basamak olabilir")
    private BigDecimal kdvOrani;
    @Digits(integer = 17, fraction = 2, message = "KDV tutarı en fazla 2 ondalık basamak olabilir")
    private BigDecimal kdvTutar;
    @Digits(integer = 17, fraction = 2, message = "Matrah en fazla 2 ondalık basamak olabilir")
    private BigDecimal matrah;
    private String odemeYontemi;
    /** Ödeme yapılan kasa; seçilirse masraf anında kasa çıkışı işlenir. */
    private Long kasaId;
    /** Ödeme yapılan banka; seçilirse masraf anında banka çıkışı işlenir. */
    private Long bankaId;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}
