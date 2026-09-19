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
    @Min(value = 2000, message = "Gecersiz yil")
    @Max(value = 2100, message = "Gecersiz yil")
    private Integer yil;
    @NotNull(message = "Ay zorunludur")
    @Min(value = 1, message = "Ay 1-12 arasinda olmalidir")
    @Max(value = 12, message = "Ay 1-12 arasinda olmalidir")
    private Integer ay;
    @DecimalMin(value = "0.0", message = "Hedef ciro negatif olamaz")
    private BigDecimal hedefCiro;
    @DecimalMin(value = "0.0", message = "Hedef kar negatif olamaz")
    private BigDecimal hedefKar;
    @Min(value = 0, message = "Hedef musteri sayisi negatif olamaz")
    private Integer hedefYeniMusteri;
    @Min(value = 0, message = "Hedef satis adedi negatif olamaz")
    private Integer hedefSatisAdedi;
    private String notlar;
    private LocalDateTime guncellemeTarihi;
}
