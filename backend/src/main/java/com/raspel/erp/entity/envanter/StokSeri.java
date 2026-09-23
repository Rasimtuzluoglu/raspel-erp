package com.raspel.erp.entity.envanter;

import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stok_seri", schema = "envanter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokSeri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stok_id", nullable = false)
    private Stok stok;

    @Column(name = "seri_no", nullable = false, length = 100)
    private String seriNo;

    @Column(name = "lot_no", length = 100)
    private String lotNo;

    @Column(name = "son_kullanma_tarihi")
    private LocalDate sonKullanmaTarihi;

    @Column(name = "depo_id")
    private Long depoId;

    @Column(precision = 19, scale = 2)
    private java.math.BigDecimal miktar;

    @Column(name = "kalan_miktar", precision = 19, scale = 2)
    private java.math.BigDecimal kalanMiktar;

    /** STOKTA | TUKETILDI */
    @Column(length = 20)
    private String durum;

    @Column(name = "giris_tarihi")
    private LocalDate girisTarihi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stok_hareket_id")
    private StokHareket stokHareket;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    /** İyimser kilitleme. */
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}
