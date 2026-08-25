package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "teklif", schema = "ticaret")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teklif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teklif_no", nullable = false, length = 50)
    private String teklifNo;

    @Column(name = "revizyon_no", nullable = false)
    @Builder.Default
    private Integer revizyonNo = 0;

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(name = "gecerlilik_tarihi")
    private LocalDate gecerlilikTarihi;

    @Column(name = "cari_hesap_id")
    private Long cariHesapId;

    @Column(length = 20)
    @Builder.Default
    private String tur = "SATIS";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String durum = "TASLAK";

    @Column(name = "ara_toplam", precision = 19, scale = 2)
    private BigDecimal araToplam;

    @Column(precision = 19, scale = 2)
    private BigDecimal kdv;

    @Column(name = "iskonto_orani", precision = 5, scale = 2)
    private BigDecimal iskontoOrani;

    @Column(name = "iskonto_tutari", precision = 19, scale = 2)
    private BigDecimal iskontoTutari;

    @Column(name = "genel_toplam", precision = 19, scale = 2)
    private BigDecimal genelToplam;

    @Column(name = "para_birimi", length = 10)
    @Builder.Default
    private String paraBirimi = "TRY";

    @Column(name = "teslimat_sarti", length = 255)
    private String teslimatSarti;

    @Column(name = "odeme_sarti", length = 255)
    private String odemeSarti;

    @Column(name = "garanti_sarti", length = 255)
    private String garantiSarti;

    @Column(columnDefinition = "TEXT")
    private String notlar;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "guncelleme_tarihi")
    private LocalDateTime guncellemeTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        guncellemeTarihi = LocalDateTime.now();
        if (durum == null) durum = "TASLAK";
        if (tur == null) tur = "SATIS";
        if (revizyonNo == null) revizyonNo = 0;
        if (paraBirimi == null) paraBirimi = "TRY";
    }

    @PreUpdate
    protected void onUpdate() {
        guncellemeTarihi = LocalDateTime.now();
    }
}
