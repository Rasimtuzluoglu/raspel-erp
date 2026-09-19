package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosTerminaliDTO {
    private Long id;
    private Long sirketId;
    @NotBlank(message = "POS adi zorunludur")
    @Size(max = 100, message = "POS adi en fazla 100 karakter olabilir")
    private String ad;
    @NotNull(message = "Banka secilmelidir")
    private Long bankaId;
    private String bankaAd;
    @DecimalMin(value = "0.0", message = "Komisyon orani negatif olamaz")
    @DecimalMax(value = "100.0", message = "Komisyon orani 100'den buyuk olamaz")
    private BigDecimal komisyonOrani;
    private Boolean aktif;
}
