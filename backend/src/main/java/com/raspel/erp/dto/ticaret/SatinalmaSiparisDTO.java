package com.raspel.erp.dto.ticaret;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SatinalmaSiparisDTO {
    private Long id;
    private String siparisNo;
    private LocalDate tarih;
    private Long cariHesapId;
    private String cariHesapAdi;
    private Long talepId;
    private String durum;
    private BigDecimal araToplam;
    private BigDecimal kdv;
    private BigDecimal genelToplam;
    private String aciklama;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
    private List<SatinalmaSiparisKalemDTO> kalemler;
}