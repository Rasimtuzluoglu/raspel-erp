package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bildirim", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bildirim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(length = 20)
    private String tur;

    @Column(length = 200)
    private String baslik;

    @Column(columnDefinition = "TEXT")
    private String mesaj;

    @Column(nullable = false)
    private Boolean okundu;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (okundu == null) okundu = false;
    }

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
