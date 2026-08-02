package com.raspel.erp.entity.muhasebe;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hesap_plani", schema = "muhasebe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HesapPlani {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String kod;

    @Column(nullable = false, length = 200)
    private String ad;

    @Column(nullable = false, length = 20)
    private String tip; // AKTIF, PASIF, GELIR, GIDER

    @Column(length = 100)
    private String grup;

    @Column(name = "ust_id")
    private Long ustId;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(nullable = false)
    private Boolean aktif;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (aktif == null) aktif = true;
    }
}
