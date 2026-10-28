package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Canlı işlem akışı olayı ({@code GET /api/dashboard/akis}).
 *
 * <p>{@code tur}: ONAY, TAHSILAT, ODEME, FATURA, SIPARIS, STOK.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardAkisDTO {

    private String tur;
    private String baslik;
    private String aciklama;
    private BigDecimal tutar;
    private LocalDateTime tarih;
    private String route;
    private String ikon;
    private String renk;
}
