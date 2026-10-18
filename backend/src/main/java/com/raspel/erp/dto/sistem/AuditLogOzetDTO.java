package com.raspel.erp.dto.sistem;

import lombok.*;

import java.util.Map;

/**
 * Denetim ekranı KPI özeti. {@code islemBazli} anahtarları işlem kodlarıdır
 * (OLUSTUR, GUNCELLE, SIL, HATA, ISLEM, ARSIVLE, ...); {@code toplam} hepsinin
 * toplamıdır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogOzetDTO {
    private long toplam;
    private Map<String, Long> islemBazli;
}
