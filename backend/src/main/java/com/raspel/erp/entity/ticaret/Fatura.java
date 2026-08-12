package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.raspel.erp.entity.finans.CariHesap;

@Entity
@Table(name = "fatura", schema = "fatura", indexes = {
    @Index(name = "idx_fatura_sirket_tarih", columnList = "sirket_id, tarih"),
    @Index(name = "idx_fatura_durum", columnList = "durum")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fatura_numarasi", nullable = false, unique = true)
    private String faturaNumarasi;

    @Column(nullable = false)
    private LocalDate tarih;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FaturaTur tur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FaturaDurum durum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cari_hesap_id", nullable = true)
    private CariHesap cariHesap;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "ara_toplam", nullable = false, precision = 19, scale = 2)
    private BigDecimal araToplam;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal kdv;

    @Column(name = "genel_toplam", nullable = false, precision = 19, scale = 2)
    private BigDecimal genelToplam;

    @Column(name = "genel_iskonto_tutari", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal genelIskontoTutari = BigDecimal.ZERO;

    @Column(name = "odeme_durumu", nullable = false, length = 20)
    @Builder.Default
    private String odemeDurumu = "ODENMEDI";

    @Column(name = "odenen_tutar", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal odenenTutar = BigDecimal.ZERO;

    @Column(name = "kalan_tutar", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal kalanTutar = BigDecimal.ZERO;

    @OneToMany(mappedBy = "fatura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FaturaKalem> kalemler = new ArrayList<>();

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "olusturan_kullanici_id")
    private Long olusturanKullaniciId;

    @Column(name = "olusturan_kullanici_adi", length = 100)
    private String olusturanKullaniciAdi;

    @Column(name = "teslim_eden", length = 100)
    private String teslimEden;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = FaturaDurum.TASLAK;
    }

    public enum FaturaTur {
        SATIS, ALIS
    }

    public enum FaturaDurum {
        TASLAK, TEKLIF, KESILDI, IPTAL
    }
}