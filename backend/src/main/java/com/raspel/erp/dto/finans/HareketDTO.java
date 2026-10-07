package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.raspel.erp.entity.finans.Hareket;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HareketDTO {

    private Long id;

    @NotNull(message = "Cari hesap seçilmelidir")
    private Long cariHesapId;

    private String cariHesapAd;

    @NotBlank(message = "Hareket türü seçilmelidir")
    private String tur;

    @NotNull(message = "Tutar girilmelidir")
    @DecimalMin(value = "0.01", message = "Tutar 0'dan büyük olmalıdır")
    @Digits(integer = 17, fraction = 2, message = "Tutar en fazla 2 ondalık basamak olabilir")
    private BigDecimal tutar;

    private LocalDate hareketTarihi;

    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
    private String aciklama;

    private String odemeSekli;

    /** Ödeme yöntemi: NAKIT, KART, TAKSIT, HAVALE */
    private String odemeYontemi;

    /** Taksit çekilen banka / finans kurumu adı */
    private String taksitKurum;

    /** Taksit olarak çekilen tutar */
    @Digits(integer = 17, fraction = 2, message = "Taksit tutarı en fazla 2 ondalık basamak olabilir")
    private BigDecimal taksitTutar;

    /** POS terminali ID (kart tek çekim) */
    private Long posTerminaliId;

    /** POS terminali adı (görüntüleme için denormalize) */
    private String posAd;

    /** Kart komisyon tutarı */
    @Digits(integer = 17, fraction = 2, message = "Komisyon tutarı en fazla 2 ondalık basamak olabilir")
    private BigDecimal komisyonTutar;

    /** Valör (bankaya geçiş) tarihi */
    private LocalDate valorTarihi;

    /** Bağlı fatura ID'si (opsiyonel): verilirse fatura ödeme durumu hareketle birlikte güncellenir */
    private Long faturaId;

    /** Açılış fişi/devir kaydı mı (Faz 2.8). */
    private Boolean acilis;

    private LocalDateTime olusturmaTarihi;
}