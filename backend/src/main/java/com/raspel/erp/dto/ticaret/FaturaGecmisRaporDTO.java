package com.raspel.erp.dto.ticaret;

import lombok.*;

import java.time.LocalDateTime;

/** Fatura işlem/yazdırma geçmişi rapor satırı (fatura bilgileriyle zenginleştirilmiş). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaturaGecmisRaporDTO {
    private Long id;
    private Long faturaId;
    private String faturaNumarasi;
    private String faturaTur;
    private String faturaDurum;
    private String cariHesapAd;
    private String olay;
    private String aciklama;
    private String kullaniciAdi;
    private String ipAdresi;
    private String yazdirmaFormat;
    private String yaziciAdi;
    private Integer kopyaNo;
    private LocalDateTime tarih;
}
