package com.raspel.erp.dto.sistem;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjeDTO {
    private Long id;
    private String ad;
    private String aciklama;
    private LocalDate baslangic;
    private LocalDate bitis;
    private String durum;
    private String sorumlu;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
    private List<GorevDTO> gorevler;
}