package com.raspel.erp.dto.sistem;

import lombok.*;

/**
 * Kural tabanlı Anasayfa içgörüsü ({@code GET /api/dashboard/icgoruler}).
 *
 * <p>{@code seviye}: INFO, WARN, KRITIK.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardIcgoruDTO {

    private String seviye;
    private String baslik;
    private String mesaj;
    private String route;
    private String ikon;
}
