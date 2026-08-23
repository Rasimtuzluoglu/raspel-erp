package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bildirim", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bildirim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(length = 20)
    private String tur;

    @Column(length = 200)
    private String baslik;

    @Column(columnDefinition = "TEXT")
    private String mesaj;

    @Column(nullable = false)
    private Boolean okundu;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (okundu == null) okundu = false;
    }
}
