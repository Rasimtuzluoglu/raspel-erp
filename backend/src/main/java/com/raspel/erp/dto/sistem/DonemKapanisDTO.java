package com.raspel.erp.dto.sistem;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonemKapanisDTO {
    private Long id;
    private Long sirketId;
    private Long donemId;
    private Integer yil;
    private LocalDateTime kapanisTarihi;
    private Long kullaniciId;
    private String ozet;
    private Integer kilitlenenDonemSayisi;
}
