package com.raspel.erp.dto.envanter;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokDTO {
    private Long id;
    private String stokKodu;
    @NotBlank(message = "Stok adı boş olamaz")
    private String ad;
    private String birim;
    @NotNull(message = "Fiyat girilmelidir")
    @DecimalMin(value = "0", message = "Fiyat 0'dan küçük olamaz")
    private BigDecimal fiyat;
    private BigDecimal satisFiyati;
    private BigDecimal miktar;
    private BigDecimal minMiktar;
    private BigDecimal kdvOrani;
    private String stokGrubu;
    private String barkod;
    private String rafNo;
    private String marka;
    private BigDecimal agirlik;
    private String kategori;
    private String aciklama;
    private String fotoUrl;
    private String birim2;
    private BigDecimal cevrimKatsayisi;
    private Long tedarikciId;
    private String tedarikciAd;
    private String tedarikciStokKodu;
    private BigDecimal tedarikciFiyat;
    private String maliyetYontemi;
    private LocalDateTime olusturmaTarihi;

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
    public BigDecimal getSatisFiyati() { return satisFiyati; }
    public void setSatisFiyati(BigDecimal satisFiyati) { this.satisFiyati = satisFiyati; }
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
    public String getTedarikciAd() { return tedarikciAd; }
    public void setTedarikciAd(String tedarikciAd) { this.tedarikciAd = tedarikciAd; }
    public String getTedarikciStokKodu() { return tedarikciStokKodu; }
    public void setTedarikciStokKodu(String tedarikciStokKodu) { this.tedarikciStokKodu = tedarikciStokKodu; }
    public BigDecimal getTedarikciFiyat() { return tedarikciFiyat; }
    public void setTedarikciFiyat(BigDecimal tedarikciFiyat) { this.tedarikciFiyat = tedarikciFiyat; }
    public String getMaliyetYontemi() { return maliyetYontemi; }
    public void setMaliyetYontemi(String maliyetYontemi) { this.maliyetYontemi = maliyetYontemi; }
    public LocalDateTime getOlusturmaTarihi() { return olusturmaTarihi; }
    public void setOlusturmaTarihi(LocalDateTime olusturmaTarihi) { this.olusturmaTarihi = olusturmaTarihi; }
}