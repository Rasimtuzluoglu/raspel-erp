package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tekrarlayan_fatura", schema = "fatura")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TekrarlayanFatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(name = "cari_hesap_id")
    private Long cariHesapId;

    @Column(nullable = false, length = 10)
    private String tur;

    @Column(length = 500)
    private String aciklama;

    @Column(nullable = false, length = 20)
    private String periyot;

    @Column(name = "baslangic_tarihi", nullable = false)
    private LocalDate baslangicTarihi;

    @Column(name = "bitis_tarihi")
    private LocalDate bitisTarihi;

    @Column(name = "sonraki_calistirma")
    private LocalDate sonrakiCalistirma;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aktif = true;

    @OneToMany(mappedBy = "tekrarlayanFatura", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TekrarlayanFaturaKalem> kalemler = new ArrayList<>();

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
        if (aktif == null) aktif = true;
        if (sonrakiCalistirma == null && baslangicTarihi != null) sonrakiCalistirma = baslangicTarihi;
    }
}
