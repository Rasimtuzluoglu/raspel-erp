package com.raspel.erp.dto.ticaret;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeklifKalemDTO {
    private Long id;
    private Long teklifId;
    private Long stokId;
    private String stokKodu;
    private String aciklama;
    private BigDecimal miktar;
    private String birim;
    private BigDecimal birimFiyat;
    private BigDecimal iskontoOrani;
    private BigDecimal kdvOrani;
    private BigDecimal tutar;
}
