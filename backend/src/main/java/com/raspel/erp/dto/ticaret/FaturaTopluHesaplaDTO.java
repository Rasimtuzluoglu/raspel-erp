package com.raspel.erp.dto.ticaret;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Geçmiş faturaların KDV-dahil modele göre toplu yeniden hesaplama özeti.
 * {@code kaydet=false} iken yalnızca önizleme (dry-run) üretilir.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaturaTopluHesaplaDTO {

    private boolean kaydet;
    private long taranan;
    private long degisecek;
    private long kilitliAtlanan;
    /** Ödenen tutarı yeni genel toplamı aşan fatura sayısı (ödenen cap'lenecek). */
    private long odemeAsan;
    private BigDecimal eskiToplam;
    private BigDecimal yeniToplam;
    private List<Ornek> ornekler;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Ornek {
        private Long id;
        private String faturaNumarasi;
        private String tarih;
        private BigDecimal eskiGenelToplam;
        private BigDecimal yeniGenelToplam;
    }
}
