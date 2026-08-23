package com.raspel.erp.dto.ticaret;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeklifKalemDTO {
    private Long id;
    private Long teklifId;
    private Long stokId;
    private String stokKodu;
    private String aciklama;
    private BigDecimal miktar;
    private String birim;
    private BigDecimal birimFiyat;
    private BigDecimal iskontoOrani;
    private BigDecimal kdvOrani;
    private BigDecimal tutar;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTeklifId() { return teklifId; }
    public void setTeklifId(Long teklifId) { this.teklifId = teklifId; }
    public Long getStokId() { return stokId; }
    public void setStokId(Long stokId) { this.stokId = stokId; }
    public String getStokKodu() { return stokKodu; }
    public void setStokKodu(String stokKodu) { this.stokKodu = stokKodu; }
    public String getAciklama() { return aciklama; }
    public void setAciklama(String aciklama) { this.aciklama = aciklama; }
    public BigDecimal getMiktar() { return miktar; }
    public void setMiktar(BigDecimal miktar) { this.miktar = miktar; }
    public String getBirim() { return birim; }
    public void setBirim(String birim) { this.birim = birim; }
    public BigDecimal getBirimFiyat() { return birimFiyat; }
    public void setBirimFiyat(BigDecimal birimFiyat) { this.birimFiyat = birimFiyat; }
    public BigDecimal getIskontoOrani() { return iskontoOrani; }
    public void setIskontoOrani(BigDecimal iskontoOrani) { this.iskontoOrani = iskontoOrani; }
    public BigDecimal getKdvOrani() { return kdvOrani; }
    public void setKdvOrani(BigDecimal kdvOrani) { this.kdvOrani = kdvOrani; }
    public BigDecimal getTutar() { return tutar; }
    public void setTutar(BigDecimal tutar) { this.tutar = tutar; }
}
