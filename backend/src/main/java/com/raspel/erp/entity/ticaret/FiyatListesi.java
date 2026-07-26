package com.raspel.erp.entity.ticaret;

import com.raspel.erp.entity.Stok;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fiyat_listesi", schema = "ticaret")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiyatListesi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stok_id", nullable = false)
    private Stok stok;

    @Column(name = "alis_fiyat", precision = 19, scale = 2)
    private BigDecimal alisFiyat;

    @Column(name = "satis_fiyat", nullable = false, precision = 19, scale = 2)
    private BigDecimal satisFiyat;

    @Column(name = "gecerli_baslangic")
    private LocalDate gecerliBaslangic;

    @Column(name = "gecerli_bitis")
    private LocalDate gecerliBitis;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}
