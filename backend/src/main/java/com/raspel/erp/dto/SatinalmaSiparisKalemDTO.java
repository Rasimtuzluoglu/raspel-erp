package com.raspel.erp.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SatinalmaSiparisKalemDTO {
    private Long id;
    private Long siparisId;
    private Long stokId;
    private String stokAdi;
    private String aciklama;
    private BigDecimal miktar;
    private String birim;
    private BigDecimal birimFiyat;
    private BigDecimal kdvOrani;
    private BigDecimal tutar;
    private LocalDateTime olusturmaTarihi;
}
