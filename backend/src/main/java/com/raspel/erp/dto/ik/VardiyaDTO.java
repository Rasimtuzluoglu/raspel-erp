package com.raspel.erp.dto.ik;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VardiyaDTO {
    private Long id;
    private Long personelId;
    private String personelAdi;
    private LocalDate tarih;
    private LocalTime baslangic;
    private LocalTime bitis;
    private String tur;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}
