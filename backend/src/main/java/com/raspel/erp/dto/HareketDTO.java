package com.raspel.erp.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    private LocalDateTime olusturmaTarihi;
}
