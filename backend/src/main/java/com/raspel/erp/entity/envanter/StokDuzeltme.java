package com.raspel.erp.entity.envanter;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stok_duzeltme", schema = "stok")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokDuzeltme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "stok_id", nullable = false)
    private Long stokId;

    @Column(name = "stok_ad", length = 255)
    private String stokAd;

    @Column(name = "eski_miktar", precision = 19, scale = 2)
    private BigDecimal eskiMiktar;

    @Column(name = "yeni_miktar", precision = 19, scale = 2)
    private BigDecimal yeniMiktar;

    @Column(length = 500)
    private String neden;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}
