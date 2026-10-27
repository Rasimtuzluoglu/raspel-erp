package com.raspel.erp.entity.muhasebe;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "irsaliye", schema = "muhasebe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Irsaliye {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "irsaliye_no", nullable = false, length = 50)
    private String irsaliyeNo;

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(name = "cari_hesap_id", nullable = true)
    private Long cariHesapId;

    @Column(name = "fatura_id")
    private Long faturaId;

    @Column(name = "siparis_id")
    private Long siparisId;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(nullable = false, length = 10)
    private String tur;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "depo_id")
    private Long depoId;

    @Column(length = 20)
    private String plaka;

    @Column(name = "sofor_id")
    private Long soforId;

    @Column(name = "teslim_alan", length = 150)
    private String teslimAlan;

    @Column(name = "teslim_tarihi")
    private LocalDateTime teslimTarihi;

    @Column(name = "teslimat_adresi", length = 500)
    private String teslimatAdresi;

    @Column(name = "koli_adeti")
    private Integer koliAdedi;

    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal desi;

    @Column(name = "brut_kg", precision = 10, scale = 2)
    private java.math.BigDecimal brutKg;

    @Column(name = "tasiyici_firma", length = 150)
    private String tasiyiciFirma;

    @Column(name = "sevk_tarihi")
    private LocalDateTime sevkTarihi;

    @Column(name = "teslimat_id")
    private Long teslimatId;

    /** İyimser kilitleme. */
    @Version
    private Long version;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = "TASLAK";
        if (tur == null) tur = "SATIS";
    }
}