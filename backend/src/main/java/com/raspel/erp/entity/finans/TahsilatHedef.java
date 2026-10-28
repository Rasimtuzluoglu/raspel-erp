package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Şirket + yıl + ay bazında tahsilat hedefi (TRY).
 * {@code (sirket_id, yil, ay)} benzersizdir; kaydetme upsert çalışır.
 */
@Entity
@Table(name = "tahsilat_hedef", schema = "finans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TahsilatHedef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false)
    private Integer yil;

    @Column(nullable = false)
    private Integer ay;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tutar;

    @Column(name = "olusturan_kullanici_id")
    private Long olusturanKullaniciId;

    @Column(name = "olusturma_tarihi", nullable = false, updatable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "guncelleme_tarihi")
    private LocalDateTime guncellemeTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
    }
}
