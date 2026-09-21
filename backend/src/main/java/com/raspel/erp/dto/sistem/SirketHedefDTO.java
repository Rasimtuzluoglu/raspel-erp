package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SirketHedefDTO {
    private Long id;
    private Long sirketId;
    @NotNull(message = "Yil zorunludur")
    @Min(value = 2000, message = "Geçersiz yıl")
    @Max(value = 2100, message = "Geçersiz yıl")
    private Integer yil;
    @NotNull(message = "Ay zorunludur")
    @Min(value = 1, message = "Ay 1-12 arasında olmalıdır")
    @Max(value = 12, message = "Ay 1-12 arasında olmalıdır")
    private Integer ay;
    @DecimalMin(value = "0.0", message = "Hedef ciro negatif olamaz")
    private BigDecimal hedefCiro;
    @DecimalMin(value = "0.0", message = "Hedef kâr negatif olamaz")
    private BigDecimal hedefKar;
    @Min(value = 0, message = "Hedef müşteri sayısı negatif olamaz")
    private Integer hedefYeniMusteri;
    @Min(value = 0, message = "Hedef satış adedi negatif olamaz")
    private Integer hedefSatisAdedi;
    private String notlar;
    private LocalDateTime guncellemeTarihi;
}
