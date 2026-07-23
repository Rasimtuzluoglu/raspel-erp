package com.raspel.erp.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
