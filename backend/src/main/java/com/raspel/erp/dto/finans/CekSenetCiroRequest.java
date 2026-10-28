package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Ciro gövdesi; hedef cari zorunlu. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CekSenetCiroRequest {
    @NotNull(message = "Ciro edilecek cari seçilmelidir")
    private Long cariId;

    @Size(max = 500, message = "Ciro notu en fazla 500 karakter olabilir")
    private String not;
}
