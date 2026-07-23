package com.raspel.erp.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaturaKalemDTO {

    private Long id;

    @NotBlank(message = "Kalem açıklaması girilmelidir")
    private String aciklama;

    @NotNull(message = "Adet girilmelidir")
    @Min(value = 1, message = "Adet en az 1 olmalıdır")
    private Integer adet;

    @NotNull(message = "Birim fiyat girilmelidir")
    @DecimalMin(value = "0.01", message = "Birim fiyat 0'dan büyük olmalıdır")
    private BigDecimal birimFiyat;

    private BigDecimal kdvOrani;

    private BigDecimal tutar;

    private Long stokId;
    private String stokAd;
    private String stokKodu;
}
