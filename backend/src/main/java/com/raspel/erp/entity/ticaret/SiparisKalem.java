package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "siparis_kalem", schema = "siparis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiparisKalem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "siparis_id", nullable = false)
    private Long siparisId;

    @Column(name = "stok_id")
    private Long stokId;

    @Column(length = 200)
    private String aciklama;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal miktar;

    @Column(name = "birim", length = 20)
    private String birim;

    @Column(name = "birim_fiyat", precision = 19, scale = 2)
    private BigDecimal birimFiyat;

    @Column(name = "kdv_orani", precision = 5, scale = 2)
    private BigDecimal kdvOrani;

    @Column(precision = 19, scale = 2)
    private BigDecimal tutar;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() { olusturmaTarihi = LocalDateTime.now(); }
}