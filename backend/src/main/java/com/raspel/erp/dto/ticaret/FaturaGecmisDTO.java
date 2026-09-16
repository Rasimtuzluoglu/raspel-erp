package com.raspel.erp.dto.ticaret;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaturaGecmisDTO {
    private Long id;
    private Long faturaId;
    private String olay;
    private String aciklama;
    private String oncekiDeger;
    private String yeniDeger;
    private Long kullaniciId;
    private String kullaniciAdi;
    private String ipAdresi;
    private String yazdirmaFormat;
    private String yaziciAdi;
    private Integer kopyaNo;
    private LocalDateTime tarih;
}
