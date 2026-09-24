package com.raspel.erp.dto.sistem;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Denetim kaydı görünümü. Entity'den farklı olarak kullanıcının görünen adını
 * (id yerine) taşır; böylece denetim ekranı anlaşılır olur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {
    private Long id;
    private Long kullaniciId;
    private String kullaniciAdi;
    private Long sirketId;
    private String islem;
    private String entityAdi;
    private Long entityId;
    private String aciklama;
    private String detay;
    private String ipAdresi;
    private LocalDateTime tarih;
}
