package com.raspel.erp.entity.ik;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Yıllık bordro hesaplama parametreleri. Asgari ücret ve vergi/SGK oranları yıl bazında
 * tanımlanır; bordro motoru hesaplamalarında bu değerler kullanılır.
 */
@Entity
@Table(name = "bordro_ayar", schema = "ik",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sirket_id", "yil"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BordroAyar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false)
    private Integer yil;

    /** Aylık brüt asgari ücret. */
    @Column(name = "asgari_ucret", nullable = false, precision = 19, scale = 2)
    private BigDecimal asgariUcret;

    @Column(name = "sgk_isci_orani", nullable = false, precision = 5, scale = 2)
    private BigDecimal sgkIsciOrani;

    @Column(name = "issizlik_isci_orani", nullable = false, precision = 5, scale = 2)
    private BigDecimal issizlikIsciOrani;

    @Column(name = "sgk_isveren_orani", nullable = false, precision = 5, scale = 2)
    private BigDecimal sgkIsverenOrani;

    @Column(name = "issizlik_isveren_orani", nullable = false, precision = 5, scale = 2)
    private BigDecimal issizlikIsverenOrani;

    @Column(name = "damga_orani", nullable = false, precision = 5, scale = 3)
    private BigDecimal damgaOrani;

    /** Gelir vergisi dilimleri JSON: [{"limit":158000,"oran":15},...]; boşsa %15 sabit. */
    @Column(name = "gelir_vergisi_dilimleri", columnDefinition = "TEXT")
    private String gelirVergisiDilimleri;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (asgariUcret == null) asgariUcret = BigDecimal.ZERO;
        if (sgkIsciOrani == null) sgkIsciOrani = new BigDecimal("14");
        if (issizlikIsciOrani == null) issizlikIsciOrani = new BigDecimal("1");
        if (sgkIsverenOrani == null) sgkIsverenOrani = new BigDecimal("20.50");
        if (issizlikIsverenOrani == null) issizlikIsverenOrani = new BigDecimal("2");
        if (damgaOrani == null) damgaOrani = new BigDecimal("0.759");
    }
}
