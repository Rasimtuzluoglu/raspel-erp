package com.raspel.erp.dto.muhasebe;

import lombok.*;

import java.time.LocalDateTime;

/** İrsaliye durum/işlem geçmişi satırı. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrsaliyeDurumLogDTO {
    private Long id;
    private String eskiDurum;
    private String yeniDurum;
    private String islem;
    private String aciklama;
    private Long kullaniciId;
    private LocalDateTime olusturmaTarihi;
}
