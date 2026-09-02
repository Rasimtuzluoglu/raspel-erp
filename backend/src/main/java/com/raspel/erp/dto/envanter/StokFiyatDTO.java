package com.raspel.erp.dto.envanter;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokFiyatDTO {
    private Long id;
    private Long stokId;
    private String ad;
    private BigDecimal fiyat;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}
