package com.raspel.erp.entity.envanter;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.raspel.erp.entity.finans.CariHesap;

@Entity
@Table(name = "stok_hareket", schema = "stok")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokHareket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stok_id", nullable = false)
    private Stok stok;

    @Column(nullable = false, length = 20)
    private String tur;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal miktar;

    @Column(name = "hareket_tarihi", nullable = false)
    private LocalDate hareketTarihi;

    @Column(length = 500)
    private String aciklama;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cari_hesap_id", nullable = true)
    private CariHesap cariHesap;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() { olusturmaTarihi = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Stok getStok() { return stok; }
    public void setStok(Stok stok) { this.stok = stok; }
    public String getTur() { return tur; }
    public void setTur(String tur) { this.tur = tur; }
    public BigDecimal getMiktar() { return miktar; }
    public void setMiktar(BigDecimal miktar) { this.miktar = miktar; }
    public LocalDate getHareketTarihi() { return hareketTarihi; }
    public void setHareketTarihi(LocalDate hareketTarihi) { this.hareketTarihi = hareketTarihi; }
    public String getAciklama() { return aciklama; }
    public void setAciklama(String aciklama) { this.aciklama = aciklama; }
    public CariHesap getCariHesap() { return cariHesap; }
    public void setCariHesap(CariHesap cariHesap) { this.cariHesap = cariHesap; }
    public LocalDateTime getOlusturmaTarihi() { return olusturmaTarihi; }
    public void setOlusturmaTarihi(LocalDateTime olusturmaTarihi) { this.olusturmaTarihi = olusturmaTarihi; }
}