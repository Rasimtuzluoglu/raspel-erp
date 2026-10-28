package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;

/**
 * Çek/senet liste KPI özeti.
 *
 * <p>Durum sayımları isteğe bağlı vade aralığına göre hesaplanır;
 * {@code vadesiGecen} ve {@code buAyVadesiGelen} bugüne göre görecelidir.
 * {@code toplamTutar} kapsamdaki tüm kayıtların tutar toplamıdır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CekSenetOzetDTO {
    private long portfoy;
    private long teminatta;
    private long ciroEdilen;
    private long tahsilEdilen;
    private long protesto;
    /** Vadesi geçmiş ve hâlâ açık (portföy/teminat) kayıt sayısı. */
    private long vadesiGecen;
    /** Bu ay vadesi gelen ve hâlâ açık (portföy/teminat) kayıt sayısı. */
    private long buAyVadesiGelen;
    private BigDecimal toplamTutar;
}
