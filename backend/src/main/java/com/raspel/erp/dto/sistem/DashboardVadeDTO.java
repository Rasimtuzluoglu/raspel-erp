package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Anasayfa vade takvimi satırı ({@code GET /api/dashboard/vadeler}).
 *
 * <p>Yalnızca vadesi seçilen aralığa düşen, kesilmiş ve ödemesi kapanmamış
 * faturalar döner. {@code kalanTutar} = genel toplam − ödenen tutar.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardVadeDTO {

    private Long faturaId;
    private String faturaNumarasi;
    private String cariAd;
    private LocalDate vadeTarihi;
    private BigDecimal kalanTutar;
    /** Vade tarihi bugünden önceyse true. */
    private Boolean geciktiMi;
}
