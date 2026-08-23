package com.raspel.erp.entity.envanter;

import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stok_seri", schema = "envanter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokSeri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stok_id", nullable = false)
    private Stok stok;

    @Column(name = "seri_no", nullable = false, length = 100)
    private String seriNo;

    @Column(name = "lot_no", length = 100)
    private String lotNo;

    @Column(name = "son_kullanma_tarihi")
    private LocalDate sonKullanmaTarihi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stok_hareket_id")
    private StokHareket stokHareket;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Stok getStok() { return stok; }
    public void setStok(Stok stok) { this.stok = stok; }
    public String getSeriNo() { return seriNo; }
    public void setSeriNo(String seriNo) { this.seriNo = seriNo; }
    public String getLotNo() { return lotNo; }
    public void setLotNo(String lotNo) { this.lotNo = lotNo; }
    public LocalDate getSonKullanmaTarihi() { return sonKullanmaTarihi; }
    public void setSonKullanmaTarihi(LocalDate sonKullanmaTarihi) { this.sonKullanmaTarihi = sonKullanmaTarihi; }
    public StokHareket getStokHareket() { return stokHareket; }
    public void setStokHareket(StokHareket stokHareket) { this.stokHareket = stokHareket; }
    public LocalDateTime getOlusturmaTarihi() { return olusturmaTarihi; }
    public void setOlusturmaTarihi(LocalDateTime olusturmaTarihi) { this.olusturmaTarihi = olusturmaTarihi; }
}
