package com.raspel.erp.entity.ik;

import com.raspel.erp.entity.Personel;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "vardiya", schema = "ik")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vardiya {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personel_id", nullable = false)
    private Personel personel;

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(nullable = false)
    private LocalTime baslangic;

    @Column(nullable = false)
    private LocalTime bitis;

    @Column(nullable = false, length = 10)
    private String tur;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}
