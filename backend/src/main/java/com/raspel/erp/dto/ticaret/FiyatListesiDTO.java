package com.raspel.erp.dto.ticaret;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiyatListesiDTO {
    private Long id;
    @NotNull(message = "Stok seçilmelidir")
    private Long stokId;
    private String stokAdi;
    private BigDecimal alisFiyat;
    @NotNull(message = "Satış fiyatı zorunludur")
    private BigDecimal satisFiyat;
    private LocalDate gecerliBaslangic;
    private LocalDate gecerliBitis;
    private Long sirketId;
    private String aciklama;
    private LocalDateTime olusturmaTarihi;
}
