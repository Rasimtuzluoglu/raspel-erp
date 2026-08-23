package com.raspel.erp.dto.envanter;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TalepTahminiDTO {
    private Long stokId;
    private String stokKodu;
    private String ad;
    private String birim;
    private BigDecimal mevcutMiktar;
    private BigDecimal gunlukOrtalamaTuketim;
    private Integer tahminiTukenmeGunu;
    private Integer tedarikSuresiGun;
    private BigDecimal onerilenSiparisMiktari;
    private String tedarikciAd;
    private String durum; // "KRITIK", "DIKKAT", "GUVENLI"
    private String proaktifOneri;

    public Long getStokId() { return stokId; }
    public void setStokId(Long stokId) { this.stokId = stokId; }
    public String getStokKodu() { return stokKodu; }
    public void setStokKodu(String stokKodu) { this.stokKodu = stokKodu; }
    public String getAd() { return ad; }
    public void setAd(String ad) { this.ad = ad; }
    public String getBirim() { return birim; }
    public void setBirim(String birim) { this.birim = birim; }
    public BigDecimal getMevcutMiktar() { return mevcutMiktar; }
    public void setMevcutMiktar(BigDecimal mevcutMiktar) { this.mevcutMiktar = mevcutMiktar; }
    public BigDecimal getGunlukOrtalamaTuketim() { return gunlukOrtalamaTuketim; }
    public void setGunlukOrtalamaTuketim(BigDecimal gunlukOrtalamaTuketim) { this.gunlukOrtalamaTuketim = gunlukOrtalamaTuketim; }
    public Integer getTahminiTukenmeGunu() { return tahminiTukenmeGunu; }
    public void setTahminiTukenmeGunu(Integer tahminiTukenmeGunu) { this.tahminiTukenmeGunu = tahminiTukenmeGunu; }
    public Integer getTedarikSuresiGun() { return tedarikSuresiGun; }
    public void setTedarikSuresiGun(Integer tedarikSuresiGun) { this.tedarikSuresiGun = tedarikSuresiGun; }
    public BigDecimal getOnerilenSiparisMiktari() { return onerilenSiparisMiktari; }
    public void setOnerilenSiparisMiktari(BigDecimal onerilenSiparisMiktari) { this.onerilenSiparisMiktari = onerilenSiparisMiktari; }
    public String getTedarikciAd() { return tedarikciAd; }
    public void setTedarikciAd(String tedarikciAd) { this.tedarikciAd = tedarikciAd; }
    public String getDurum() { return durum; }
    public void setDurum(String durum) { this.durum = durum; }
    public String getProaktifOneri() { return proaktifOneri; }
    public void setProaktifOneri(String proaktifOneri) { this.proaktifOneri = proaktifOneri; }
}
