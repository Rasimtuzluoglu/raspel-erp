package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Tek kayıt için onay/red isteği. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnayKararRequest {

    @NotNull(message = "Onay türü zorunludur")
    private String tur;

    @NotNull(message = "Kaynak kayıt seçilmelidir")
    private Long kaynakId;

    @NotBlank(message = "Karar zorunludur (ONAY veya RED)")
    private String karar;

    @Size(max = 500, message = "Gerekçe en fazla 500 karakter olabilir")
    private String gerekce;
}
