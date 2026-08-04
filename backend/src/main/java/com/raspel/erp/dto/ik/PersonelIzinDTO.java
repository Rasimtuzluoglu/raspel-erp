package com.raspel.erp.dto.ik;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonelIzinDTO {
    private Long id;
    private Long personelId;
    private String personelAdi;
    private String izinTuru;
    private LocalDate baslangic;
    private LocalDate bitis;
    private Integer gunSayisi;
    private String durum;
    private String aciklama;
    private String onaylayan;
    private LocalDateTime olusturmaTarihi;
}