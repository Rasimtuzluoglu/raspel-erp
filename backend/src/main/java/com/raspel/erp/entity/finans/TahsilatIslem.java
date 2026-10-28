package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tahsilat işlemi (batch). Tek bir tahsilat girişinde oluşan tüm
 * {@code cari.hareket} satırlarını (fatura dağıtımı + fazla/avans) birleştirir.
 *
 * <p>İptal, işlem bütünlüğü içinde yapılır: işlemdeki her hareket
 * {@code HareketService.hareketIptal} ile geri alınır, işlem {@code IPTAL}
 * olarak işaretlenir. Kısmi iptal yoktur.
 */
@Entity
@Table(name = "tahsilat_islem", schema = "finans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TahsilatIslem {

    public static final String DURUM_AKTIF = "AKTIF";
    public static final String DURUM_IPTAL = "IPTAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(name = "cari_id", nullable = false)
    private Long cariId;

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tutar;

    @Column(name = "odeme_yontemi", length = 20)
    private String odemeYontemi;

    @Column(name = "kasa_id")
    private Long kasaId;

    @Column(name = "banka_id")
    private Long bankaId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String durum = DURUM_AKTIF;

    @Column(name = "iptal_gerekce", length = 500)
    private String iptalGerekce;

    @Column(name = "iptal_eden_kullanici_id")
    private Long iptalEdenKullaniciId;

    @Column(name = "iptal_tarihi")
    private LocalDateTime iptalTarihi;

    @Column(name = "olusturan_kullanici_id")
    private Long olusturanKullaniciId;

    /** Yalnızca legacy backfill eşlemesi; unique. */
    @Column(name = "legacy_hareket_id")
    private Long legacyHareketId;

    @Column(name = "olusturma_tarihi", nullable = false, updatable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = DURUM_AKTIF;
    }

    public boolean aktifMi() {
        return DURUM_AKTIF.equals(durum);
    }
}
