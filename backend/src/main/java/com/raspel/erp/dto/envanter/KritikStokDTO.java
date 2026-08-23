package com.raspel.erp.dto.envanter;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KritikStokDTO {
    private Long id;
    private String stokKodu;
    private String ad;
    private String birim;
    private BigDecimal miktar;
    private BigDecimal minMiktar;
    private BigDecimal onerilenSiparisMiktari;
    private String kategori;
    private String marka;
    private String tedarikciAd;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStokKodu() { return stokKodu; }
    public void setStokKodu(String stokKodu) { this.stokKodu = stokKodu; }
    public String getAd() { return ad; }
    public void setAd(String ad) { this.ad = ad; }
    public String getBirim() { return birim; }
    public void setBirim(String birim) { this.birim = birim; }
    public BigDecimal getMiktar() { return miktar; }
    public void setMiktar(BigDecimal miktar) { this.miktar = miktar; }
    public BigDecimal getMinMiktar() { return minMiktar; }
    public void setMinMiktar(BigDecimal minMiktar) { this.minMiktar = minMiktar; }
    public BigDecimal getOnerilenSiparisMiktari() { return onerilenSiparisMiktari; }
    public void setOnerilenSiparisMiktari(BigDecimal onerilenSiparisMiktari) { this.onerilenSiparisMiktari = onerilenSiparisMiktari; }
    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }
    public String getMarka() { return marka; }
    public void setMarka(String marka) { this.marka = marka; }
    public String getTedarikciAd() { return tedarikciAd; }
    public void setTedarikciAd(String tedarikciAd) { this.tedarikciAd = tedarikciAd; }
}