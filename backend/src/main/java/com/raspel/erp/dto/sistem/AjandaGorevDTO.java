package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjandaGorevDTO {
    private Long id;
    @NotBlank(message = "Gorev basligi zorunludur")
    @Size(max = 200, message = "Baslik en fazla 200 karakter olabilir")
    private String baslik;
    @Size(max = 2000, message = "Aciklama en fazla 2000 karakter olabilir")
    private String aciklama;
    private LocalDate bitisTarihi;
    private String oncelik; // DUSUK | ORTA | YUKSEK
    private String durum;   // BEKLIYOR | TAMAMLANDI | IPTAL
    private LocalDateTime olusturmaTarihi;
}
