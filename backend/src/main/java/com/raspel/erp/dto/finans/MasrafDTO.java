package com.raspel.erp.dto.finans;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasrafDTO {
    private Long id;
    private LocalDate tarih;
    private BigDecimal tutar;
    private String aciklama;
    private String kategori;
    private Long cariHesapId;
    private String belgeNo;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}
