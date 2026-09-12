package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaksitDTO {
    private Long id;
    private Long sirketId;
    private Long cariId;
    private String cariAd;
    private Long faturaId;
    private Long hareketId;
    private String planNo;
    private String kurum;
    private Integer taksitNo;
    private Integer taksitSayisi;
    private LocalDate vadeTarihi;
    private BigDecimal tutar;
    private String odemeDurumu;
    private LocalDate odemeTarihi;
    private String aciklama;
    /** Vadesi gecmis gun sayisi (gecikme yoksa 0). */
    private long gecikmeGunu;
    private LocalDateTime olusturmaTarihi;
}
