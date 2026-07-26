package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "yetki", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Yetki {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kod", nullable = false, unique = true, length = 50)
    private String kod; // STOK_READ, STOK_WRITE, STOK_DELETE, FATURA_READ, FATURA_WRITE, FATURA_DELETE, CARI_READ, CARI_WRITE, CARI_DELETE, EXPORT_DATA

    @Column(name = "modul", nullable = false, length = 50)
    private String modul; // Stok, Fatura, Cari, Personel, Sistem

    @Column(name = "aciklama", length = 150)
    private String aciklama;
}
