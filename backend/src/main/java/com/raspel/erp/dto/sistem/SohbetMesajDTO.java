package com.raspel.erp.dto.sistem;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SohbetMesajDTO {
    private Long id;
    private Long sirketId;
    private Long kullaniciId;
    private String kullaniciAd;
    private Long odaId;
    private String mesaj;
    private LocalDateTime olusturmaTarihi;
}
