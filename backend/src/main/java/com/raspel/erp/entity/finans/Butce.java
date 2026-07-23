package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "butce", schema = "finans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Butce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String ad;

    @Column(nullable = false)
    private Integer yil;

    @Column(nullable = false)
    private Integer ay;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tutar;

    @Column(nullable = false, length = 10)
    private String tur;

    @Column(length = 100)
    private String kategori;

    @Column(name = "sirket_id")
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
