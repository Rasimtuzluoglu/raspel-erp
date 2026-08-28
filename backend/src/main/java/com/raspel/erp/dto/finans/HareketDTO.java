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
    private BigDecimal tutar;

    private LocalDate hareketTarihi;

    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
    private String aciklama;

    private String odemeSekli;

    /** Bağlı fatura ID'si (opsiyonel): verilirse fatura ödeme durumu hareketle birlikte güncellenir */
    private Long faturaId;

    private LocalDateTime olusturmaTarihi;
}