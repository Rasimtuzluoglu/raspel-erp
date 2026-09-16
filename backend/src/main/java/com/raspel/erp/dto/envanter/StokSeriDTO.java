package com.raspel.erp.dto.envanter;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokSeriDTO {
    private Long id;
    private Long stokId;
    private String stokAdi;
    private String seriNo;
    private String lotNo;
    private LocalDate sonKullanmaTarihi;
    private Long depoId;
    private java.math.BigDecimal miktar;
    private java.math.BigDecimal kalanMiktar;
    private String durum;
    private LocalDate girisTarihi;
    private Long stokHareketId;
    private LocalDateTime olusturmaTarihi;
}
