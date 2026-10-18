package com.raspel.erp.dto.ticaret;

import lombok.*;

/**
 * Sipariş Takibi ekranı KPI özeti. Sayaçlar şirket kapsamındadır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiparisTakipOzetDTO {
    private long toplam;
    /** Teslim edilmemiş/iptal edilmemiş siparişler. */
    private long bekleyen;
    /** Aktif üretim emri bulunan (tamamlanmamış) sipariş sayısı. */
    private long uretimde;
    /** Sevkte (YOLDA) siparişler. */
    private long sevkte;
    private long teslimEdildi;
    /** Beklenen teslim tarihi geçmiş, teslim edilmemiş siparişler. */
    private long geciken;
    private long iptal;
}
