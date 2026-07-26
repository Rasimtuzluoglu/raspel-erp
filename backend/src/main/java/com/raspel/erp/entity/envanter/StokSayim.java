package com.raspel.erp.entity.envanter;

import com.raspel.erp.entity.Stok;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stok_sayim", schema = "envanter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokSayim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate tarih;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stok_id", nullable = false)
    private Stok stok;

    @Column(name = "beklenen_miktar", nullable = false, precision = 19, scale = 2)
    private BigDecimal beklenenMiktar;

    @Column(name = "sayilan_miktar", nullable = false, precision = 19, scale = 2)
    private BigDecimal sayilanMiktar;

    @Column(precision = 19, scale = 2)
    private BigDecimal fark;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (beklenenMiktar == null) beklenenMiktar = BigDecimal.ZERO;
        if (sayilanMiktar == null) sayilanMiktar = BigDecimal.ZERO;
        if (durum == null) durum = "TASLAK";
    }
}
