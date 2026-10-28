package com.raspel.erp.dto.finans;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/** Hedef–performans sekmesi: aylık hedef, gerçekleşme ve 6 aylık trend. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TahsilatPerformansDTO {

    /** Seçili ayın hedefi; tanımlı değilse null. */
    private BigDecimal hedef;

    /** Seçili ayın iptal edilmemiş tahsilat toplamı. */
    private BigDecimal tahsilEdilen;

    /** Yüzde; hedef yok/sıfır ise null (sıfıra bölme yapılmaz). */
    private BigDecimal gerceklesmeYuzde;

    /** Seçili aydaki aktif tahsilat işlem sayısı. */
    private long tahsilatSayisi;

    /** Vadesi geçmiş açık fatura toplamı (global). */
    private BigDecimal vadesiGecmisToplam;

    /** Son 6 ay (seçili ay dahil) tahsilat/hedef trendi. */
    private List<Trend> trend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Trend {
        /** YYYY-MM */
        private String ay;
        private BigDecimal tahsilat;
        private BigDecimal hedef;
    }
}
