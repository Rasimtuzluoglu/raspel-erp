package com.raspel.erp.dto.sistem;

import lombok.*;

import java.time.LocalDateTime;

/** Onay karar geçmişi kaydı. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnayKararDTO {
    private Long id;
    private String tur;
    private Long kaynakId;
    private String karar;
    private String gerekce;
    private String oncekiDurum;
    private String yeniDurum;
    private Long kararVerenId;
    private String kararVerenAd;
    private LocalDateTime olusturmaTarihi;
}
