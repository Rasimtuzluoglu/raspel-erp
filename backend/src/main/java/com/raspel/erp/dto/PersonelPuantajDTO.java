package com.raspel.erp.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonelPuantajDTO {
    private Long id;
    private Long personelId;
    private String personelAdi;
    private LocalDate tarih;
    private String durum;
    private String aciklama;
    private LocalDateTime olusturmaTarihi;
}
