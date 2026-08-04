package com.raspel.erp.dto.ik;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonelDTO {
    private Long id;
    private String ad;
    private String soyad;
    private String tcKimlik;
    private LocalDate dogumTarihi;
    private LocalDate iseGirisTarihi;
    private LocalDate cikisTarihi;
    private String departman;
    private String pozisyon;
    private BigDecimal maas;
    private String telefon;
    private String email;
    private String adres;
    private Boolean aktif;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}