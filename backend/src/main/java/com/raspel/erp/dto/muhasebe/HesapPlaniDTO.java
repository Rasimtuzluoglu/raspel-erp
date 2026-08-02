package com.raspel.erp.dto.muhasebe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HesapPlaniDTO {
    private Long id;

    @NotBlank(message = "Hesap kodu girilmelidir")
    private String kod;

    @NotBlank(message = "Hesap adı girilmelidir")
    private String ad;

    @NotBlank(message = "Hesap tipi girilmelidir")
    private String tip; // AKTIF, PASIF, GELIR, GIDER

    private String grup;

    private Long ustId;

    @NotNull(message = "Şirket bilgisi zorunludur")
    private Long sirketId;

    private Boolean aktif;
}
