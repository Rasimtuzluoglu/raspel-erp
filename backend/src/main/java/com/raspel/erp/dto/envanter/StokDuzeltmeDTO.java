package com.raspel.erp.dto.envanter;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokDuzeltmeDTO {
    private Long id;
    private Long sirketId;
    private Long stokId;
    private String stokAd;
    private BigDecimal eskiMiktar;
    private BigDecimal yeniMiktar;
    private String neden;
    private Long kullaniciId;
    private LocalDateTime olusturmaTarihi;
}
