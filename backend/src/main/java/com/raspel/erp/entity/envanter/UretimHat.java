package com.raspel.erp.entity.envanter;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Üretim hattı / iş merkezi. Emirler bir hatta atanır; yönetim görünümünde
 * hat doluluğu (aktif emir / kapasite) ve geciken emir sayısı izlenir.
 */
@Entity
@Table(name = "uretim_hat", schema = "stok")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimHat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false, length = 120)
    private String ad;

    @Column(length = 500)
    private String aciklama;

    /** Aynı anda açık tutulabilecek emir sayısı. */
    @Column(nullable = false)
    @Builder.Default
    private Integer kapasite = 5;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aktif = true;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
        if (kapasite == null) kapasite = 5;
        if (aktif == null) aktif = true;
    }
}
