package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Kasa gün sonu (Z raporu) verisi. Sunucu tarafında hesaplanır; kasa hareketleri,
 * tahsilat ödeme yöntemi kırılımı ve günün satış toplamları birlikte döner.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KasaGunSonuDTO {

    private LocalDate tarih;
    private Long kasaId;
    private String kasaAd;

    /** Gün başı bakiye (o günden önceki tüm hareketlerin neti). */
    private BigDecimal acilisBakiye;
    private BigDecimal gunIciGiris;
    private BigDecimal gunIciCikis;
    /** Beklenen nakit (acilis + giris - cikis). */
    private BigDecimal kapanisBakiye;

    private BigDecimal tahsilatToplam;
    private long tahsilatAdedi;
    private BigDecimal nakitTahsilat;
    private BigDecimal kartTahsilat;
    private BigDecimal havaleTahsilat;
    private BigDecimal taksitTahsilat;
    private BigDecimal digerTahsilat;

    private BigDecimal giderToplam;
    private long giderAdedi;

    /** Bu kasaya (fatura.kasaId) ödenen günün satışları. */
    private long satisAdedi;
    private BigDecimal satisToplam;

    private List<Satir> hareketler = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Satir {
        private Long id;
        private String tur;
        private String aciklama;
        private BigDecimal tutar;
        private String kaynakTip;
        private String odemeYontemi;
    }
}
