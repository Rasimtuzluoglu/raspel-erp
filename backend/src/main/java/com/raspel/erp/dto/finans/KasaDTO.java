package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.raspel.erp.entity.finans.Kasa;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KasaDTO {
    private Long id;
    @NotBlank(message = "Kasa adı boş olamaz")
    private String ad;
    private BigDecimal bakiye;
    private LocalDateTime olusturmaTarihi;
}