package com.raspel.erp.dto.finans;

import lombok.*;

import java.time.LocalDateTime;

/** Çek/senet durum geçmişi satırı (zaman çizelgesi). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CekSenetDurumLogDTO {
    private Long id;
    private Long cekSenetId;
    private Long sirketId;
    private String eskiDurum;
    private String yeniDurum;
    /** OLUSTUR, GUNCELLE, TEMINAT, TEMINAT_IADE, CIRO, CIRO_IADE, IADE_AL, TAHSIL, TAHSIL_IPTAL, PROTESTO. */
    private String islem;
    private Long kullaniciId;
    private String aciklama;
    private LocalDateTime olusturmaTarihi;
}
