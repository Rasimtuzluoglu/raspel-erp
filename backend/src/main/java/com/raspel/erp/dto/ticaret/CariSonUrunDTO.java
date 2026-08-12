package com.raspel.erp.dto.ticaret;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariSonUrunDTO {
    private Long stokId;
    private String stokKodu;
    private String stokAd;
    private LocalDate sonAlisTarihi;
    private BigDecimal sonBirimFiyat;
    private Long adet;
}
