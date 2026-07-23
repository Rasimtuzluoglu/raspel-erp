package com.raspel.erp.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GorevDTO {
    private Long id;
    private Long projeId;
    private String ad;
    private String aciklama;
    private String durum;
    private String atanan;
    private LocalDate baslangic;
    private LocalDate bitis;
    private LocalDateTime olusturmaTarihi;
}
