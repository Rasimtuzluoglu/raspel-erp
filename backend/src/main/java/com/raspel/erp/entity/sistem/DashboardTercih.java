package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Kullanıcı bazlı Anasayfa düzeni.
 *
 * <p>{@code duzen} alanı serbest biçimli bir JSON object'tir
 * ({@code {versiyon:1, sira:[{key, boyut, gorunur}]}}). Doğrulama ve boyut
 * sınırı servis katmanında uygulanır; entity yalnızca saklar.
 *
 * <p>{@code kullaniciId} UNIQUE: bir kullanıcının tek düzeni olur. Tenant
 * izolasyonu {@code sirketId} ile sağlanır.
 */
@Entity
@Table(name = "dashboard_tercih", schema = "sistem",
        uniqueConstraints = {@UniqueConstraint(name = "uk_dashboard_tercih_kullanici", columnNames = {"kullanici_id"})})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardTercih {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kullanici_id", nullable = false)
    private Long kullaniciId;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    /** JSON object (en fazla 20 KB, servis katmanında doğrulanır). */
    @Column(name = "duzen", nullable = false, columnDefinition = "TEXT")
    private String duzen;

    @Column(name = "guncelleme_tarihi", nullable = false)
    private LocalDateTime guncellemeTarihi;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        guncellemeTarihi = LocalDateTime.now();
    }
}
