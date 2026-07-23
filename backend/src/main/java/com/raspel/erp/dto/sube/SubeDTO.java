package com.raspel.erp.dto.sube;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubeDTO {
    private Long id;
    private String ad;
    private String adres;
    private String telefon;
    private String yetkili;
    private Long sirketId;
    private Boolean aktif;
    private LocalDateTime olusturmaTarihi;
}
