package com.raspel.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "belge", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Belge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_adi", nullable = false, length = 50)
    private String entityAdi;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "dosya_adi", nullable = false, length = 255)
    private String dosyaAdi;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}
