package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasrafOzetDTO {
    private BigDecimal buAyToplam;
    private BigDecimal buYilToplam;
    private BigDecimal buYilKdv;
    private long toplamKayit;
}
