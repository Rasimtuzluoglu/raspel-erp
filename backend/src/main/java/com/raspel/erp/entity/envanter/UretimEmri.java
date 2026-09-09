package com.raspel.erp.entity.envanter;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "uretim_emri", schema = "stok")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimEmri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "urun_id", nullable = false)
    private Long urunId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal miktar;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "tamamlanma_tarihi")
    private LocalDateTime tamamlanmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = Durum.TASLAK.name();
    }

    public enum Durum {
        TASLAK, URETIMDE, TAMAMLANDI, IPTAL
    }
}
