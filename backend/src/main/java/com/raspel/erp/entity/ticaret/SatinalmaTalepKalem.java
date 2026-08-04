package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "satinalma_talep_kalem", schema = "satinalma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SatinalmaTalepKalem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "talep_id", nullable = false)
    private Long talepId;

    @Column(name = "stok_id")
    private Long stokId;

    @Column(length = 200)
    private String aciklama;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal miktar;

    @Column(name = "birim", length = 20)
    private String birim;

    @Column(name = "tahmini_birim_fiyat", precision = 19, scale = 2)
    private BigDecimal tahminiBirimFiyat;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() { olusturmaTarihi = LocalDateTime.now(); }
}