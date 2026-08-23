package com.raspel.erp.dto.envanter;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokSayimDTO {
    private Long id;
    private LocalDate tarih;
    private Long stokId;
    private String stokAdi;
    private BigDecimal beklenenMiktar;
    private BigDecimal sayilanMiktar;
    private BigDecimal fark;
    private String durum;
    private Long sirketId;
    private String aciklama;
    private LocalDateTime olusturmaTarihi;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getTarih() { return tarih; }
    public void setTarih(LocalDate tarih) { this.tarih = tarih; }
    public Long getStokId() { return stokId; }
    public void setStokId(Long stokId) { this.stokId = stokId; }
    public String getStokAdi() { return stokAdi; }
    public void setStokAdi(String stokAdi) { this.stokAdi = stokAdi; }
    public BigDecimal getBeklenenMiktar() { return beklenenMiktar; }
    public void setBeklenenMiktar(BigDecimal beklenenMiktar) { this.beklenenMiktar = beklenenMiktar; }
    public BigDecimal getSayilanMiktar() { return sayilanMiktar; }
    public void setSayilanMiktar(BigDecimal sayilanMiktar) { this.sayilanMiktar = sayilanMiktar; }
    public BigDecimal getFark() { return fark; }
    public void setFark(BigDecimal fark) { this.fark = fark; }
    public String getDurum() { return durum; }
    public void setDurum(String durum) { this.durum = durum; }
    public Long getSirketId() { return sirketId; }
    public void setSirketId(Long sirketId) { this.sirketId = sirketId; }
    public String getAciklama() { return aciklama; }
    public void setAciklama(String aciklama) { this.aciklama = aciklama; }
    public LocalDateTime getOlusturmaTarihi() { return olusturmaTarihi; }
    public void setOlusturmaTarihi(LocalDateTime olusturmaTarihi) { this.olusturmaTarihi = olusturmaTarihi; }
}
