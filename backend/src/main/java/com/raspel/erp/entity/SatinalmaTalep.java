package com.raspel.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "satinalma_talep", schema = "satinalma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SatinalmaTalep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "talep_no", nullable = false, unique = true, length = 50)
    private String talepNo;

    @Column(name = "tarih", nullable = false)
    private LocalDate tarih;

    @Column(name = "talep_eden", length = 100)
    private String talepEden;

    @Column(name = "departman", length = 100)
    private String departman;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = "TASLAK";
    }
}
