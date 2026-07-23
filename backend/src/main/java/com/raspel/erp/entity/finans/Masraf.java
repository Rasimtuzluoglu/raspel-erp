package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "masraf", schema = "finans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Masraf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tutar;

    @Column(length = 500)
    private String aciklama;

    @Column(length = 100)
    private String kategori;

    @Column(name = "cari_hesap_id")
    private Long cariHesapId;

    @Column(name = "belge_no", length = 50)
    private String belgeNo;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}
