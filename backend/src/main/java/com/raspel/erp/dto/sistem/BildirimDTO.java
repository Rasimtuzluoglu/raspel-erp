package com.raspel.erp.dto.sistem;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BildirimDTO {
    private Long id;
    private Long sirketId;
    private String tur;
    private String baslik;
    private String mesaj;
    private Boolean okundu;
    private LocalDateTime olusturmaTarihi;
}
