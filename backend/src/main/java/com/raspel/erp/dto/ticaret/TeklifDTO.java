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
public class TeklifDTO {
    private Long id;
    private String teklifNo;
    private Integer revizyonNo;
    private LocalDate tarih;
    private LocalDate gecerlilikTarihi;
    private Long cariHesapId;
    private String cariHesapAdi;
    private String cariVergiNo;
    private String cariVergiDairesi;
    private String cariTelefon;
    private String cariEmail;
    private String cariAdres;
    private String tur;
    private String durum;
    private BigDecimal araToplam;
    private BigDecimal kdv;
    private BigDecimal iskontoOrani;
    private BigDecimal iskontoTutari;
    private BigDecimal genelToplam;
    private String paraBirimi;
    private String teslimatSarti;
    private String odemeSarti;
    private String garantiSarti;
    private String notlar;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
    private List<TeklifKalemDTO> kalemler;
}
