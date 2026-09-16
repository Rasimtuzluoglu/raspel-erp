package com.raspel.erp.entity.envanter;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Ağırlıklı ortalama maliyet defteri satırı (giriş/çıkış). */
@Entity
@Table(name = "stok_maliyet_hareket", schema = "maliyet", indexes = {
    @Index(name = "idx_smh_stok", columnList = "stok_id, tarih"),
    @Index(name = "idx_smh_sirket", columnList = "sirket_id, tarih")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokMaliyetHareket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stok_id", nullable = false)
    private Long stokId;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(nullable = false)
    private LocalDate tarih;

    /** GIRIS | CIKIS */
    @Column(nullable = false, length = 10)
    private String tur;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal miktar;

    @Column(name = "birim_maliyet", nullable = false, precision = 19, scale = 4)
    private BigDecimal birimMaliyet;

    @Column(name = "toplam_maliyet", nullable = false, precision = 19, scale = 2)
    private BigDecimal toplamMaliyet;

    @Column(name = "kalan_miktar", nullable = false, precision = 19, scale = 4)
    private BigDecimal kalanMiktar;

    @Column(name = "ortalama_maliyet", nullable = false, precision = 19, scale = 4)
    private BigDecimal ortalamaMaliyet;

    @Column(name = "kaynak_tip", length = 30)
    private String kaynakTip;

    @Column(name = "kaynak_id")
    private Long kaynakId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
    }
}
