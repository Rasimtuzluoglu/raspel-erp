package com.raspel.erp.dto.sistem;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Kullanıcı Anasayfa düzeni.
 *
 * <p>{@code GET /api/dashboard/tercihler} kayıt yoksa {@code duzen = null}
 * döner. {@code PUT} gövdesi de aynı DTO'dur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardTercihDTO {

    /** Serbest biçimli JSON object (null ise kullanıcı henüz kaydetmemiştir). */
    private Map<String, Object> duzen;

    private LocalDateTime guncellemeTarihi;
}
