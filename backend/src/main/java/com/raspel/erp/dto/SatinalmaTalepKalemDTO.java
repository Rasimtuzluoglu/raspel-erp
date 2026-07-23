package com.raspel.erp.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SatinalmaTalepKalemDTO {
    private Long id;
    private Long talepId;
    private Long stokId;
    private String stokAdi;
    private String aciklama;
    private BigDecimal miktar;
    private String birim;
    private BigDecimal tahminiBirimFiyat;
    private LocalDateTime olusturmaTarihi;
}
