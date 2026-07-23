package com.raspel.erp.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SatinalmaTalepDTO {
    private Long id;
    private String talepNo;
    private LocalDate tarih;
    private String talepEden;
    private String departman;
    private String durum;
    private String aciklama;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
    private List<SatinalmaTalepKalemDTO> kalemler;
}
