package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sifre_sifirla_token", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SifreSifirlaToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kullanici_id", nullable = false)
    private Long kullaniciId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "son_kullanma", nullable = false)
    private LocalDateTime sonKullanma;

    @Column(nullable = false)
    @Builder.Default
    private Boolean kullanildi = false;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
        if (kullanildi == null) kullanildi = false;
    }
}
