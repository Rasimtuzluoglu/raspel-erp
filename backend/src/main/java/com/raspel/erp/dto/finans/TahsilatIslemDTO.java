package com.raspel.erp.dto.finans;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tahsilat işlemi (batch) görünümü. Bir işlem; fatura dağıtımları ve
 * fazla/avans satırlarını {@link Dagilim} listesinde taşır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TahsilatIslemDTO {

    private Long id;
    private LocalDate tarih;
    private Long cariId;
    private String cariAd;
    private BigDecimal tutar;
    private String odemeYontemi;
    private String kasaAd;
    private String bankaAd;
    /** AKTIF / IPTAL */
    private String durum;
    private String iptalGerekce;
    private Long olusturanKullaniciId;
    private int satirSayisi;
    private List<Dagilim> dagilimlar;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Dagilim {
        private Long hareketId;
        private Long faturaId;
        private String faturaNumarasi;
        private BigDecimal tutar;
        private String aciklama;
        private boolean iptal;
    }
}
