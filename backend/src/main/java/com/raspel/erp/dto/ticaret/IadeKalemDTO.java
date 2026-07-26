package com.raspel.erp.dto.ticaret;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IadeKalemDTO {
    private Long id;
    private Long stokId;
    private String stokAd;
    private String stokKodu;
    private String aciklama;
    private BigDecimal miktar;
    private String birim;
    private BigDecimal birimFiyat;
    private BigDecimal kdvOrani;
    private BigDecimal tutar;
}
