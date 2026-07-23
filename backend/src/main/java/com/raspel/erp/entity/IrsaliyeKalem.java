package com.raspel.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "irsaliye_kalem", schema = "muhasebe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrsaliyeKalem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "irsaliye_id", nullable = false)
    private Long irsaliyeId;

    @Column(name = "stok_id")
    private Long stokId;

    @Column(length = 200)
    private String aciklama;

    @Column(nullable = false)
    private Double miktar;

    @Column(length = 20)
    private String birim;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() { olusturmaTarihi = LocalDateTime.now(); }
}
