package com.raspel.erp.entity.muhasebe;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "muhasebe_fisi", schema = "muhasebe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MuhasebeFisi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fis_no", nullable = false, length = 30)
    private String fisNo;

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(length = 500)
    private String aciklama;

    @Column(nullable = false, length = 20)
    private String durum; // KAYITLI, ONAYLANDI, IPTAL

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = "KAYITLI";
    }
}
