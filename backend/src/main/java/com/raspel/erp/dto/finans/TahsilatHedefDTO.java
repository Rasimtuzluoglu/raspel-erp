package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

/** Aylık tahsilat hedefi istek/yanıt modeli. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TahsilatHedefDTO {

    @NotNull(message = "Yıl seçilmelidir")
    @Min(value = 2000, message = "Yıl 2000 ile 2100 arasında olmalıdır")
    @Max(value = 2100, message = "Yıl 2000 ile 2100 arasında olmalıdır")
    private Integer yil;

    @NotNull(message = "Ay seçilmelidir")
    @Min(value = 1, message = "Ay 1 ile 12 arasında olmalıdır")
    @Max(value = 12, message = "Ay 1 ile 12 arasında olmalıdır")
    private Integer ay;

    @NotNull(message = "Hedef tutarı girilmelidir")
    @DecimalMin(value = "0.00", message = "Hedef tutarı negatif olamaz")
    private BigDecimal tutar;
}
