package com.raspel.erp.entity.envanter;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.raspel.erp.entity.finans.CariHesap;

@Entity
@Table(name = "stok_hareket", schema = "stok")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokHareket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stok_id", nullable = false)
    private Stok stok;

    @Column(nullable = false, length = 20)
    private String tur;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal miktar;

    @Column(name = "hareket_tarihi", nullable = false)
    private LocalDate hareketTarihi;

    @Column(length = 500)
    private String aciklama;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cari_hesap_id", nullable = true)
    private CariHesap cariHesap;

    /** Hareketin deposu (biliniyorsa). */
    @Column(name = "depo_id")
    private Long depoId;

    /** Kaynak belge tipi: FATURA, IRSALIYE, URETIM, SAYIM, DUZELTME, IADE, TRANSFER, MANUEL. */
    @Column(name = "kaynak_tip", length = 30)
    private String kaynakTip;

    /** Kaynak belge kimliği. */
    @Column(name = "kaynak_id")
    private Long kaynakId;

    /** Seri/lot kaydı (seri takibi yapılan ürünlerde). */
    @Column(name = "seri_id")
    private Long seriId;

    /** İyimser kilitleme. */
    @Version
    private Long version;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() { olusturmaTarihi = LocalDateTime.now(); }
}