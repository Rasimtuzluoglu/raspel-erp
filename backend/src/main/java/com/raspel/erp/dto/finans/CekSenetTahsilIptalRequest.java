package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Tahsil iptali gövdesi; gerekçe zorunlu ve en az 3 karakter. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CekSenetTahsilIptalRequest {
    @NotBlank(message = "Tahsil iptali için gerekçe girilmelidir")
    @Size(min = 3, max = 500, message = "Gerekçe 3-500 karakter arasında olmalıdır")
    private String gerekce;
}
