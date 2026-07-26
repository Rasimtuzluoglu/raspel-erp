package com.raspel.erp.dto.sistem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomaliDTO {
    private String id;
    private String tur; // MUKERRER_FATURA, MUKERRER_ODEME, ANORMAL_MASRAF, ANORMAL_STOK_CIKISI
    private String seviye; // YUKSEK, ORTA, DUSUK
    private String baslik;
    private String aciklama;
    private Long ilgiliKayitId;
    private String oneri;
    private LocalDateTime tespitTarihi;
}
