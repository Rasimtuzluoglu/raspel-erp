package com.raspel.erp.dto.sistem;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BildirimDTO {
    private Long id;
    private Long sirketId;
    private String tur;
    private String baslik;
    private String mesaj;
    private Boolean okundu;
    private LocalDateTime olusturmaTarihi;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSirketId() { return sirketId; }
    public void setSirketId(Long sirketId) { this.sirketId = sirketId; }
    public String getTur() { return tur; }
    public void setTur(String tur) { this.tur = tur; }
    public String getBaslik() { return baslik; }
    public void setBaslik(String baslik) { this.baslik = baslik; }
    public String getMesaj() { return mesaj; }
    public void setMesaj(String mesaj) { this.mesaj = mesaj; }
    public Boolean getOkundu() { return okundu; }
    public void setOkundu(Boolean okundu) { this.okundu = okundu; }
    public LocalDateTime getOlusturmaTarihi() { return olusturmaTarihi; }
    public void setOlusturmaTarihi(LocalDateTime olusturmaTarihi) { this.olusturmaTarihi = olusturmaTarihi; }
}
