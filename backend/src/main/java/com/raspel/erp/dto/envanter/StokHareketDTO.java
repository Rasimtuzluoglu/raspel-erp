package com.raspel.erp.dto.envanter;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StokHareketDTO {
    private Long id;
    private Long stokId;
    private String stokAd;
    private String stokKodu;
    @NotBlank(message = "Hareket türü seçilmelidir")
    private String tur;
    @NotNull(message = "Miktar girilmelidir")
    private BigDecimal miktar;
    @NotNull(message = "Tarih girilmelidir")
    private LocalDate hareketTarihi;
    private String aciklama;
    private Long cariHesapId;
    private String cariHesapAd;
    private LocalDateTime olusturmaTarihi;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStokId() { return stokId; }
    public void setStokId(Long stokId) { this.stokId = stokId; }
    public String getStokAd() { return stokAd; }
    public void setStokAd(String stokAd) { this.stokAd = stokAd; }
    public String getStokKodu() { return stokKodu; }
    public void setStokKodu(String stokKodu) { this.stokKodu = stokKodu; }
    public String getTur() { return tur; }
    public void setTur(String tur) { this.tur = tur; }
    public BigDecimal getMiktar() { return miktar; }
    public void setMiktar(BigDecimal miktar) { this.miktar = miktar; }
    public LocalDate getHareketTarihi() { return hareketTarihi; }
    public void setHareketTarihi(LocalDate hareketTarihi) { this.hareketTarihi = hareketTarihi; }
    public String getAciklama() { return aciklama; }
    public void setAciklama(String aciklama) { this.aciklama = aciklama; }
    public Long getCariHesapId() { return cariHesapId; }
    public void setCariHesapId(Long cariHesapId) { this.cariHesapId = cariHesapId; }
    public String getCariHesapAd() { return cariHesapAd; }
    public void setCariHesapAd(String cariHesapAd) { this.cariHesapAd = cariHesapAd; }
    public LocalDateTime getOlusturmaTarihi() { return olusturmaTarihi; }
    public void setOlusturmaTarihi(LocalDateTime olusturmaTarihi) { this.olusturmaTarihi = olusturmaTarihi; }
}