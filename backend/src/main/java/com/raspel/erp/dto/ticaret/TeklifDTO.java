package com.raspel.erp.dto.ticaret;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeklifDTO {
    private Long id;
    private String teklifNo;
    private Integer revizyonNo;
    private LocalDate tarih;
    private LocalDate gecerlilikTarihi;
    private Long cariHesapId;
    private String cariHesapAdi;
    private String cariVergiNo;
    private String cariVergiDairesi;
    private String cariTelefon;
    private String cariEmail;
    private String cariAdres;
    private String tur;
    private String durum;
    private BigDecimal araToplam;
    private BigDecimal kdv;
    private BigDecimal iskontoOrani;
    private BigDecimal iskontoTutari;
    private BigDecimal genelToplam;
    private String paraBirimi;
    private String teslimatSarti;
    private String odemeSarti;
    private String garantiSarti;
    private String notlar;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
    private List<TeklifKalemDTO> kalemler;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTeklifNo() { return teklifNo; }
    public void setTeklifNo(String teklifNo) { this.teklifNo = teklifNo; }
    public Integer getRevizyonNo() { return revizyonNo; }
    public void setRevizyonNo(Integer revizyonNo) { this.revizyonNo = revizyonNo; }
    public LocalDate getTarih() { return tarih; }
    public void setTarih(LocalDate tarih) { this.tarih = tarih; }
    public LocalDate getGecerlilikTarihi() { return gecerlilikTarihi; }
    public void setGecerlilikTarihi(LocalDate gecerlilikTarihi) { this.gecerlilikTarihi = gecerlilikTarihi; }
    public Long getCariHesapId() { return cariHesapId; }
    public void setCariHesapId(Long cariHesapId) { this.cariHesapId = cariHesapId; }
    public String getCariHesapAdi() { return cariHesapAdi; }
    public void setCariHesapAdi(String cariHesapAdi) { this.cariHesapAdi = cariHesapAdi; }
    public String getCariVergiNo() { return cariVergiNo; }
    public void setCariVergiNo(String cariVergiNo) { this.cariVergiNo = cariVergiNo; }
    public String getCariVergiDairesi() { return cariVergiDairesi; }
    public void setCariVergiDairesi(String cariVergiDairesi) { this.cariVergiDairesi = cariVergiDairesi; }
    public String getCariTelefon() { return cariTelefon; }
    public void setCariTelefon(String cariTelefon) { this.cariTelefon = cariTelefon; }
    public String getCariEmail() { return cariEmail; }
    public void setCariEmail(String cariEmail) { this.cariEmail = cariEmail; }
    public String getCariAdres() { return cariAdres; }
    public void setCariAdres(String cariAdres) { this.cariAdres = cariAdres; }
    public String getTur() { return tur; }
    public void setTur(String tur) { this.tur = tur; }
    public String getDurum() { return durum; }
    public void setDurum(String durum) { this.durum = durum; }
    public BigDecimal getAraToplam() { return araToplam; }
    public void setAraToplam(BigDecimal araToplam) { this.araToplam = araToplam; }
    public BigDecimal getKdv() { return kdv; }
    public void setKdv(BigDecimal kdv) { this.kdv = kdv; }
    public BigDecimal getIskontoOrani() { return iskontoOrani; }
    public void setIskontoOrani(BigDecimal iskontoOrani) { this.iskontoOrani = iskontoOrani; }
    public BigDecimal getIskontoTutari() { return iskontoTutari; }
    public void setIskontoTutari(BigDecimal iskontoTutari) { this.iskontoTutari = iskontoTutari; }
    public BigDecimal getGenelToplam() { return genelToplam; }
    public void setGenelToplam(BigDecimal genelToplam) { this.genelToplam = genelToplam; }
    public String getParaBirimi() { return paraBirimi; }
    public void setParaBirimi(String paraBirimi) { this.paraBirimi = paraBirimi; }
    public String getTeslimatSarti() { return teslimatSarti; }
    public void setTeslimatSarti(String teslimatSarti) { this.teslimatSarti = teslimatSarti; }
    public String getOdemeSarti() { return odemeSarti; }
    public void setOdemeSarti(String odemeSarti) { this.odemeSarti = odemeSarti; }
    public String getGarantiSarti() { return garantiSarti; }
    public void setGarantiSarti(String garantiSarti) { this.garantiSarti = garantiSarti; }
    public String getNotlar() { return notlar; }
    public void setNotlar(String notlar) { this.notlar = notlar; }
    public Long getSirketId() { return sirketId; }
    public void setSirketId(Long sirketId) { this.sirketId = sirketId; }
    public LocalDateTime getOlusturmaTarihi() { return olusturmaTarihi; }
    public void setOlusturmaTarihi(LocalDateTime olusturmaTarihi) { this.olusturmaTarihi = olusturmaTarihi; }
    public List<TeklifKalemDTO> getKalemler() { return kalemler; }
    public void setKalemler(List<TeklifKalemDTO> kalemler) { this.kalemler = kalemler; }
}
