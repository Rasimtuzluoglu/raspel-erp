package com.raspel.erp.dto.finans;

import lombok.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasrafDTO {
    private Long id;
    @NotNull(message = "Tarih zorunludur")
    private LocalDate tarih;
    @NotNull(message = "Tutar zorunludur")
    @DecimalMin(value = "0", message = "Tutar negatif olamaz")
    private BigDecimal tutar;
    private String aciklama;
    private String kategori;
    private Long cariHesapId;
    private String belgeNo;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}
