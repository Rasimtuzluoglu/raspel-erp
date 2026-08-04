package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.raspel.erp.entity.sistem.GelirGiderKategori;

@Entity
@Table(name = "kasa_hareket", schema = "muhasebe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KasaHareket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kasa_id", nullable = false)
    private Kasa kasa;

    @Column(nullable = false, length = 20)
    private String tur;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tutar;

    @Column(name = "hareket_tarihi", nullable = false)
    private LocalDate hareketTarihi;

    @Column(length = 500)
    private String aciklama;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kategori_id")
    private GelirGiderKategori kategori;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}