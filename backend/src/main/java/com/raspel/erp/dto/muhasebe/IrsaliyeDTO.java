package com.raspel.erp.dto.muhasebe;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrsaliyeDTO {
    private Long id;
    private String irsaliyeNo;
    private LocalDate tarih;
    private Long cariHesapId;
    private String cariHesapAdi;
    private Long faturaId;
    private Long siparisId;
    private String durum;
    private String tur;
    private String aciklama;
    private Long sirketId;
    private Long depoId;
    private LocalDateTime olusturmaTarihi;
    private List<IrsaliyeKalemDTO> kalemler;

    private String plaka;
    private Long soforId;
    private String soforAd;
    private String teslimAlan;
    private LocalDateTime teslimTarihi;
    private String teslimatAdresi;
    private Integer koliAdedi;
    private java.math.BigDecimal desi;
    private java.math.BigDecimal brutKg;
    private String tasiyiciFirma;
    private LocalDateTime sevkTarihi;
    private Long teslimatId;
    private String teslimatDurum;
    private String siparisNo;
    private String faturaNumarasi;
    private List<IrsaliyeDurumLogDTO> gecmis;
}