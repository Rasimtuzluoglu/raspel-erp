package com.raspel.erp.dto.sistem;

import lombok.*;
import java.time.LocalDate;

/**
 * Ajanda olayı: belirli bir tarihte gerçekleşecek görev, vade vb.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjandaOlayDTO {
    private LocalDate tarih;
    private String tip;      // VADE | GOREV
    private String baslik;
    private String aciklama;
}
