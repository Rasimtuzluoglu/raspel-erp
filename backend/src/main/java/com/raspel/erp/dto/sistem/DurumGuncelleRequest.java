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

    /** Tahsilat için opsiyonel kasa hesabı (yalnızca çek/senet tahsilinde kullanılır). */
    private Long kasaId;

    /** Tahsilat için opsiyonel banka hesabı (yalnızca çek/senet tahsilinde kullanılır). */
    private Long bankaId;
}
