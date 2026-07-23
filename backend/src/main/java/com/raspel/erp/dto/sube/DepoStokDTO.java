package com.raspel.erp.dto.sube;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepoStokDTO {
    private Long id;
    private Long depoId;
    private String depoAdi;
    private Long stokId;
    private String stokAd;
    private String stokKodu;
    private String birim;
    private BigDecimal miktar;
    private LocalDateTime olusturmaTarihi;
}
