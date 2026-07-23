package com.raspel.erp.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonemDTO {
    private Long id;
    private Long sirketId;
    private String ad;
    private LocalDate baslangic;
    private LocalDate bitis;
    private Boolean aktif;
    private LocalDateTime olusturmaTarihi;
}
