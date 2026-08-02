package com.raspel.erp.dto.muhasebe;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefteriKebirSatiriDTO {
    private LocalDate tarih;
    private String fisNo;
    private String aciklama;
    private BigDecimal borc;
    private BigDecimal alacak;
    private BigDecimal bakiye;
}
