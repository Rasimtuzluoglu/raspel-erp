package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DurumGuncelleRequest {
    @NotBlank(message = "Durum boş olamaz")
    private String durum;

    private String onaylayan;
}
