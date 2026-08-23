package com.raspel.erp.entity.envanter;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stok", schema = "stok")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stok {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stok_kodu", length = 50)
    private String stokKodu;

    @Column(nullable = false, length = 300)
    private String ad;

    @Column(length = 50)
    private String birim;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fiyat;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal miktar;

    @Column(name = "min_miktar", precision = 19, scale = 2)
    private BigDecimal minMiktar;

    @Column(name = "kdv_orani", precision = 5, scale = 2)
    private BigDecimal kdvOrani;

    @Column(name = "stok_grubu", length = 100)
    private String stokGrubu;

    @Column(length = 100)
    private String barkod;

    @Column(name = "raf_no", length = 50)
    private String rafNo;

    @Column(length = 100)
    private String marka;

    @Column(name = "satis_fiyati", precision = 19, scale = 2)
    private BigDecimal satisFiyati;

    @Column(precision = 10, scale = 2)
    private BigDecimal agirlik;

    @Column(length = 100)
    private String kategori;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(length = 50)
    private String birim2;

    @Column(name = "cevrim_katsayisi", precision = 19, scale = 4)
    private BigDecimal cevrimKatsayisi;

    @Column(name = "tedarikci_id")
    private Long tedarikciId;

    @Column(name = "tedarikci_stok_kodu", length = 100)
    private String tedarikciStokKodu;

    @Column(name = "tedarikci_fiyat", precision = 19, scale = 2)
    private BigDecimal tedarikciFiyat;

    @Column(name = "maliyet_yontemi", length = 20)
    private String maliyetYontemi;

    @Version
    private Integer version;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (miktar == null) miktar = BigDecimal.ZERO;
        if (fiyat == null) fiyat = BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStokKodu() { return stokKodu; }
    public void setStokKodu(String stokKodu) { this.stokKodu = stokKodu; }
    public String getAd() { return ad; }
    public void setAd(String ad) { this.ad = ad; }
    public String getBirim() { return birim; }
    public void setBirim(String birim) { this.birim = birim; }
    public BigDecimal getFiyat() { return fiyat; }
    public void setFiyat(BigDecimal fiyat) { this.fiyat = fiyat; }
    public BigDecimal getMiktar() { return miktar; }
    public void setMiktar(BigDecimal miktar) { this.miktar = miktar; }
    public BigDecimal getMinMiktar() { return minMiktar; }
    public void setMinMiktar(BigDecimal minMiktar) { this.minMiktar = minMiktar; }
    public BigDecimal getKdvOrani() { return kdvOrani; }
    public void setKdvOrani(BigDecimal kdvOrani) { this.kdvOrani = kdvOrani; }
    public String getStokGrubu() { return stokGrubu; }
    public void setStokGrubu(String stokGrubu) { this.stokGrubu = stokGrubu; }
    public String getBarkod() { return barkod; }
    public void setBarkod(String barkod) { this.barkod = barkod; }
    public String getRafNo() { return rafNo; }
    public void setRafNo(String rafNo) { this.rafNo = rafNo; }
    public String getMarka() { return marka; }
    public void setMarka(String marka) { this.marka = marka; }
    public BigDecimal getSatisFiyati() { return satisFiyati; }
    public void setSatisFiyati(BigDecimal satisFiyati) { this.satisFiyati = satisFiyati; }
    public BigDecimal getAgirlik() { return agirlik; }
    public void setAgirlik(BigDecimal agirlik) { this.agirlik = agirlik; }
    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }
    public String getAciklama() { return aciklama; }
    public void setAciklama(String aciklama) { this.aciklama = aciklama; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public String getBirim2() { return birim2; }
    public void setBirim2(String birim2) { this.birim2 = birim2; }
    public BigDecimal getCevrimKatsayisi() { return cevrimKatsayisi; }
    public void setCevrimKatsayisi(BigDecimal cevrimKatsayisi) { this.cevrimKatsayisi = cevrimKatsayisi; }
    public Long getTedarikciId() { return tedarikciId; }
    public void setTedarikciId(Long tedarikciId) { this.tedarikciId = tedarikciId; }
    public String getTedarikciStokKodu() { return tedarikciStokKodu; }
    public void setTedarikciStokKodu(String tedarikciStokKodu) { this.tedarikciStokKodu = tedarikciStokKodu; }
    public BigDecimal getTedarikciFiyat() { return tedarikciFiyat; }
    public void setTedarikciFiyat(BigDecimal tedarikciFiyat) { this.tedarikciFiyat = tedarikciFiyat; }
    public String getMaliyetYontemi() { return maliyetYontemi; }
    public void setMaliyetYontemi(String maliyetYontemi) { this.maliyetYontemi = maliyetYontemi; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Long getSirketId() { return sirketId; }
    public void setSirketId(Long sirketId) { this.sirketId = sirketId; }
    public LocalDateTime getOlusturmaTarihi() { return olusturmaTarihi; }
    public void setOlusturmaTarihi(LocalDateTime olusturmaTarihi) { this.olusturmaTarihi = olusturmaTarihi; }
}