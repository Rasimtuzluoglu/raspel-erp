package com.raspel.erp.dto.sistem;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjandaGorevDTO {
    private Long id;
    private String baslik;
    private String aciklama;
    private LocalDate bitisTarihi;
    private String oncelik; // DUSUK | ORTA | YUKSEK
    private String durum;   // BEKLIYOR | TAMAMLANDI | IPTAL
    private LocalDateTime olusturmaTarihi;
}
