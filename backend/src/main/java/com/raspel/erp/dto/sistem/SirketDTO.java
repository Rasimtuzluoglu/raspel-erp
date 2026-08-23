package com.raspel.erp.dto.sistem;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SirketDTO {
    private Long id;
    private String ad;
    private String vergiNo;
    private String vergiDairesi;
    private String adres;
    private String telefon;
    private String email;
    private String webSite;
    private String logoUrl;
    private Long parentId;
    private String tur;
    private Integer yil;
    private Boolean aktif;
    private LocalDateTime olusturmaTarihi;
    private LocalDateTime sonAdGuncellemeTarihi;
}