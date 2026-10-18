package com.raspel.erp.dto.ticaret;

import lombok.*;

/**
 * Teslimatlar ekranı KPI özeti. Sayaçlar şirket kapsamındadır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeslimatOzetDTO {
    private long toplam;
    private long bekleyen;
    private long yolda;
    private long teslimEdildi;
    private long iptal;
    /** Beklenen teslim tarihi geçmiş, teslim edilmemiş/iptal edilmemiş teslimatlar. */
    private long geciken;
}
