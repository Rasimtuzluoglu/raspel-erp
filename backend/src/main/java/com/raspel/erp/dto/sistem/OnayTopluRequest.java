package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/** Aynı türde birden çok kayıt için toplu onay/red isteği. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnayTopluRequest {

    @NotNull(message = "Onay türü zorunludur")
    private String tur;

    @NotEmpty(message = "En az bir kayıt seçilmelidir")
    private List<Long> kaynakIds;

    @NotBlank(message = "Karar zorunludur (ONAY veya RED)")
    private String karar;

    @Size(max = 500, message = "Gerekçe en fazla 500 karakter olabilir")
    private String gerekce;
}
