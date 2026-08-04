package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.raspel.erp.entity.finans.Banka;

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