package com.raspel.erp.entity.ik;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "personel_puantaj", schema = "personel")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonelPuantaj {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "personel_id", nullable = false)
    private Long personelId;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(length = 200)
    private String aciklama;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() { olusturmaTarihi = LocalDateTime.now(); }
}