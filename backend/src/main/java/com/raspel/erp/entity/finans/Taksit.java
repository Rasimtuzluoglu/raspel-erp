package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Taksit Entity
 * Bir cari hesap icin vade bazli olusturulan taksit plan kalemlerini temsil eder.
 */
@Entity
@Table(name = "taksit", schema = "finans", indexes = {
    @Index(name = "idx_taksit_sirket_vade", columnList = "sirket_id, vade_tarihi, odeme_durumu"),
    @Index(name = "idx_taksit_sirket_cari", columnList = "sirket_id, cari_id"),
    @Index(name = "idx_taksit_plan_no", columnList = "plan_no")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Taksit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tenant (sirket) kimligi */
    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    /** Taksitin ait oldugu cari hesap */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cari_id", nullable = false)
    private CariHesap cariHesap;

    /** Planin iliskili oldugu fatura (opsiyonel) */
    @Column(name = "fatura_id")
    private Long faturaId;

    /** Odemeyi temsil eden hareket (opsiyonel) */
    @Column(name = "hareket_id")
    private Long hareketId;

    /** Ayni plana ait kalemleri gruplayan numara */
    @Column(name = "plan_no", nullable = false, length = 40)
    private String planNo;

    /** Taksit cekilen banka / finans kurumu */
    @Column(length = 255)
    private String kurum;

    /** Kacinci taksit (1 tabanli) */
    @Column(name = "taksit_no", nullable = false)
    private Integer taksitNo;

    /** Toplam taksit sayisi */
    @Column(name = "taksit_sayisi", nullable = false)
    private Integer taksitSayisi;

    /** Taksit vade tarihi */
    @Column(name = "vade_tarihi", nullable = false)
    private LocalDate vadeTarihi;

    /** Taksit tutari */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tutar;

    /** Odeme durumu: BEKLEMEDE, ODENDI */
    @Column(name = "odeme_durumu", nullable = false, length = 20)
    private String odemeDurumu;

    /** Odemenin yapildigi tarih */
    @Column(name = "odeme_tarihi")
    private LocalDate odemeTarihi;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "olusturma_tarihi", nullable = false, updatable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (this.olusturmaTarihi == null) {
            this.olusturmaTarihi = LocalDateTime.now();
        }
        if (this.odemeDurumu == null) {
            this.odemeDurumu = "BEKLEMEDE";
        }
    }
}
