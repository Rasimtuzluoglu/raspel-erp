package com.raspel.erp.dto.finans;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankaHareketiDTO {
    private Long id;
    private Long bankaId;
    private LocalDate tarih;
    private String aciklama;
    private BigDecimal borc;
    private BigDecimal alacak;
    private BigDecimal bakiye;
    private Long eslesenFaturaId;
    private String eslesenFaturaNo;
    private Boolean eslestirildi;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}
