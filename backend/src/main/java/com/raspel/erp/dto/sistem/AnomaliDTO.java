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
    private String tur; // MUKERRER_FATURA, MUKERRER_ODEME, YUKSEK_TUTAR
    private String seviye; // YUKSEK, ORTA, DUSUK
    private String baslik;
    private String aciklama;
    private Long ilgiliKayitId;
    /** İlgili belge numarası (varsa; ör. fatura no) — çapraz gezinme için. */
    private String ilgiliKayitNo;
    private String oneri;
    /** Kalıcı inceleme durumu: null (açık) | INCELENDI | YOK_SAYILDI. */
    private String durum;
    private LocalDateTime tespitTarihi;
}
