package com.raspel.erp.dto.muhasebe;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MuhasebeFisKalemDTO {
    private Long id;

    @NotBlank(message = "Hesap kodu girilmelidir")
    private String hesapKodu;

    private String hesapAdi;

    private BigDecimal borc;

    private BigDecimal alacak;

    private String aciklama;
}
