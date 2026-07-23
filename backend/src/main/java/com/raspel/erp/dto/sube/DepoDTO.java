package com.raspel.erp.dto.sube;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepoDTO {
    private Long id;
    private String ad;
    private String adres;
    private String yetkili;
    private Long subeId;
    private String subeAdi;
    private Long sirketId;
    private Boolean aktif;
    private LocalDateTime olusturmaTarihi;
}
