package com.raspel.erp.entity.envanter;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "uretim_emri", schema = "stok")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimEmri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "siparis_id")
    private Long siparisId;

    @Column(name = "urun_id", nullable = false)
    private Long urunId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal miktar;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "tamamlanma_tarihi")
    private LocalDateTime tamamlanmaTarihi;

    @Column(name = "planlanan_baslangic")
    private LocalDate planlananBaslangic;

    @Column(name = "planlanan_bitis")
    private LocalDate planlananBitis;

    @Column(name = "baslama_tarihi")
    private LocalDateTime baslamaTarihi;

    @Column(length = 10)
    @Builder.Default
    private String oncelik = "NORMAL";

    @Column(name = "uretilen_miktar", precision = 19, scale = 2)
    private BigDecimal uretilenMiktar;

    @Column(name = "fire_miktar", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal fireMiktar = BigDecimal.ZERO;

    @Column(name = "sorumlu_personel_id")
    private Long sorumluPersonelId;

    @Column(name = "depo_id")
    private Long depoId;

    @Column(name = "hammadde_maliyeti", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal hammaddeMaliyeti = BigDecimal.ZERO;

    @Column(name = "iscilik_maliyeti", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal iscilikMaliyeti = BigDecimal.ZERO;

    @Column(name = "toplam_maliyet", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal toplamMaliyet = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notlar;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = Durum.TASLAK.name();
    }

    public enum Durum {
        TASLAK, URETIMDE, TAMAMLANDI, IPTAL
    }
}
