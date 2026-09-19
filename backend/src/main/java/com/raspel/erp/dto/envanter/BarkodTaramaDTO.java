package com.raspel.erp.dto.envanter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarkodTaramaDTO {
    @NotBlank(message = "Barkod zorunludur")
    private String barkod;
    @DecimalMin(value = "0.01", message = "Adet sifirdan buyuk olmalidir")
    private BigDecimal adet;
}