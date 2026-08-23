package com.raspel.erp.dto.envanter;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokSeriDTO {
    private Long id;
    private Long stokId;
    private String stokAdi;
    private String seriNo;
    private String lotNo;
    private LocalDate sonKullanmaTarihi;
    private Long stokHareketId;
    private LocalDateTime olusturmaTarihi;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStokId() { return stokId; }
    public void setStokId(Long stokId) { this.stokId = stokId; }
    public String getStokAdi() { return stokAdi; }
    public void setStokAdi(String stokAdi) { this.stokAdi = stokAdi; }
    public String getSeriNo() { return seriNo; }
    public void setSeriNo(String seriNo) { this.seriNo = seriNo; }
    public String getLotNo() { return lotNo; }
    public void setLotNo(String lotNo) { this.lotNo = lotNo; }
    public LocalDate getSonKullanmaTarihi() { return sonKullanmaTarihi; }
    public void setSonKullanmaTarihi(LocalDate sonKullanmaTarihi) { this.sonKullanmaTarihi = sonKullanmaTarihi; }
    public Long getStokHareketId() { return stokHareketId; }
    public void setStokHareketId(Long stokHareketId) { this.stokHareketId = stokHareketId; }
    public LocalDateTime getOlusturmaTarihi() { return olusturmaTarihi; }
    public void setOlusturmaTarihi(LocalDateTime olusturmaTarihi) { this.olusturmaTarihi = olusturmaTarihi; }
}
