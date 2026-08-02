package com.raspel.erp.dto.muhasebe;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MizanSatiriDTO {
    private String hesapKodu;
    private String hesapAdi;
    private BigDecimal borc;
    private BigDecimal alacak;
    private BigDecimal borcBakiye;
    private BigDecimal alacakBakiye;
}
