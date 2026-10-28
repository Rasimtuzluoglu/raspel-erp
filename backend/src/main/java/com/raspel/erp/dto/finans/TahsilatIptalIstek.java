package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Tahsilat işlemi iptal isteği; gerekçe zorunlu (min 3 karakter). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TahsilatIptalIstek {

    @NotBlank(message = "İptal gerekçesi zorunludur")
    @Size(min = 3, max = 500, message = "İptal gerekçesi 3 ile 500 karakter arasında olmalıdır")
    private String gerekce;
}
