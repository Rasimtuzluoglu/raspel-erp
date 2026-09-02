package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariFiyatDTO {
    private Long id;
    private Long cariHesapId;
    private Long stokId;
    private String stokAd;
    private String stokKodu;
    private BigDecimal fiyat;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}
