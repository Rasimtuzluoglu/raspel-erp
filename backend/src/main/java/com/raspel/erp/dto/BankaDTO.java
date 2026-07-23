package com.raspel.erp.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankaDTO {

    private Long id;

    @NotBlank(message = "Banka adı boş olamaz")
    private String ad;

    private String hesapNo;
    private String iban;
    private BigDecimal bakiye;
    private LocalDateTime olusturmaTarihi;
}
