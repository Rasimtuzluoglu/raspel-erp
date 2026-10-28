package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;

/**
 * Anasayfa görev merkezi kartı ({@code GET /api/dashboard/gorevler}).
 *
 * <p>{@code tur}: ONAY_IZIN, ONAY_MASRAF, ONAY_SATINALMA, ONAY_SIPARIS,
 * VADESI_GECEN, VADESI_YAKLASAN, KRITIK_STOK, BEKLEYEN_TESLIMAT.
 * Liste önceliğe göre azalan sırada döner.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardGorevDTO {

    private String tur;
    private String baslik;
    private String aciklama;
    private Long adet;
    private BigDecimal tutar;
    /** 0-100; büyük değer daha önce gösterilir. */
    private Integer oncelik;
    private String route;
    private String ikon;
    private String renk;
}
