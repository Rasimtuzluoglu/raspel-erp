package com.raspel.erp.dto.finans;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.raspel.erp.entity.sube.Sube;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CekSenetDTO {
    private Long id;
    private String tur;
    private Long cariHesapId;
    private String cariHesapAdi;
    private String bankaAdi;
    private String sube;
    private String cekNo;
    private String hesapNo;
    private LocalDate vadeTarihi;
    private LocalDate kesinmeTarihi;
    private BigDecimal tutar;
    private String durum;
    private String aciklama;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}