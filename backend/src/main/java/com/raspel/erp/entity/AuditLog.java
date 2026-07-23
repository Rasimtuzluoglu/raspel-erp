package com.raspel.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(nullable = false, length = 50)
    private String islem;

    @Column(name = "entity_adi", length = 100)
    private String entityAdi;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(columnDefinition = "TEXT")
    private String aciklama;

    @Column(name = "ip_adresi", length = 50)
    private String ipAdresi;

    @Column(nullable = false)
    private LocalDateTime tarih;

    @PrePersist
    protected void onCreate() {
        if (tarih == null) tarih = LocalDateTime.now();
    }
}
