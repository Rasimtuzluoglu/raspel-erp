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
    private String tip;      // VADE | GOREV | KISISEL_GOREV | HATIRLATICI
    private String baslik;
    private String aciklama;
    /** Kaynak kaydın kimliği (fatura/proje görevi/kişisel görev/hatırlatıcı). */
    private Long kaynakId;
    /** Kaynak türü: FATURA | PROJE_GOREVI | KISISEL_GOREV | HATIRLATICI. */
    private String kaynakTip;
    /** Fatura gibi numarayla açılan kaynaklar için görünen numara. */
    private String kaynakNo;
}
