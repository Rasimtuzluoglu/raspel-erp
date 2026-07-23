package com.raspel.erp.dto;

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
    private String durum;
    private String tur;
    private String aciklama;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
    private List<IrsaliyeKalemDTO> kalemler;
}
