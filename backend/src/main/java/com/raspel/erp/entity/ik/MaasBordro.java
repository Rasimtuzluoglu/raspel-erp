package com.raspel.erp.entity.ik;

import com.raspel.erp.entity.Personel;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "maas_bordro", schema = "ik")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaasBordro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personel_id", nullable = false)
    private Personel personel;

    @Column(nullable = false)
    private Integer yil;

    @Column(nullable = false)
    private Integer ay;

    @Column(name = "brut_maas", nullable = false, precision = 19, scale = 2)
    private BigDecimal brutMaas;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal kesintiler;

    @Column(name = "net_maas", nullable = false, precision = 19, scale = 2)
    private BigDecimal netMaas;

    @Column(name = "odeme_tarihi")
    private LocalDate odemeTarihi;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (kesintiler == null) kesintiler = BigDecimal.ZERO;
    }
}
