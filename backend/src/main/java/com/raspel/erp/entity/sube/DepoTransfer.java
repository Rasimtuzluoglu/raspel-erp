package com.raspel.erp.entity.sube;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "depo_transfer", schema = "sube")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepoTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(name = "kaynak_depo_id", nullable = false)
    private Long kaynakDepoId;

    @Column(name = "hedef_depo_id", nullable = false)
    private Long hedefDepoId;

    @Column(name = "stok_id", nullable = false)
    private Long stokId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal miktar;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "olusturan_kullanici_id")
    private Long olusturanKullaniciId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "onay_tarihi")
    private LocalDateTime onayTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = "BEKLIYOR";
    }
}
