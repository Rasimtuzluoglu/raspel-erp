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
    private Long stokHareketId;
    private LocalDateTime olusturmaTarihi;
}
