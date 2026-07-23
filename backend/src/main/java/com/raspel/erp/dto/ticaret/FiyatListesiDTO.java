package com.raspel.erp.dto.ticaret;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiyatListesiDTO {
    private Long id;
    private Long stokId;
    private String stokAdi;
    private BigDecimal alisFiyat;
    private BigDecimal satisFiyat;
    private LocalDate gecerliBaslangic;
    private LocalDate gecerliBitis;
    private Long sirketId;
    private String aciklama;
    private LocalDateTime olusturmaTarihi;
}
