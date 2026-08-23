package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "teklif_kalem", schema = "ticaret")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeklifKalem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teklif_id", nullable = false)
    private Long teklifId;

    @Column(name = "stok_id")
    private Long stokId;

    @Column(nullable = false, length = 500)
    private String aciklama;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal miktar = BigDecimal.ONE;

    @Column(length = 20)
    @Builder.Default
    private String birim = "Adet";

    @Column(name = "birim_fiyat", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal birimFiyat = BigDecimal.ZERO;

    @Column(name = "iskonto_orani", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal iskontoOrani = BigDecimal.ZERO;

    @Column(name = "kdv_orani", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal kdvOrani = new BigDecimal("20");

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal tutar = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTeklifId() { return teklifId; }
    public void setTeklifId(Long teklifId) { this.teklifId = teklifId; }
    public Long getStokId() { return stokId; }
    public void setStokId(Long stokId) { this.stokId = stokId; }
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

    public static TeklifKalemBuilder builder() {
        return new TeklifKalemBuilder();
    }

    public static class TeklifKalemBuilder {
        private Long id;
        private Long teklifId;
        private Long stokId;
        private String aciklama;
        private BigDecimal miktar = BigDecimal.ONE;
        private String birim = "Adet";
        private BigDecimal birimFiyat = BigDecimal.ZERO;
        private BigDecimal iskontoOrani = BigDecimal.ZERO;
        private BigDecimal kdvOrani = new BigDecimal("20");
        private BigDecimal tutar = BigDecimal.ZERO;

        TeklifKalemBuilder() {}

        public TeklifKalemBuilder id(Long id) { this.id = id; return this; }
        public TeklifKalemBuilder teklifId(Long teklifId) { this.teklifId = teklifId; return this; }
        public TeklifKalemBuilder stokId(Long stokId) { this.stokId = stokId; return this; }
        public TeklifKalemBuilder aciklama(String aciklama) { this.aciklama = aciklama; return this; }
        public TeklifKalemBuilder miktar(BigDecimal miktar) { this.miktar = miktar; return this; }
        public TeklifKalemBuilder birim(String birim) { this.birim = birim; return this; }
        public TeklifKalemBuilder birimFiyat(BigDecimal birimFiyat) { this.birimFiyat = birimFiyat; return this; }
        public TeklifKalemBuilder iskontoOrani(BigDecimal iskontoOrani) { this.iskontoOrani = iskontoOrani; return this; }
        public TeklifKalemBuilder kdvOrani(BigDecimal kdvOrani) { this.kdvOrani = kdvOrani; return this; }
        public TeklifKalemBuilder tutar(BigDecimal tutar) { this.tutar = tutar; return this; }

        public TeklifKalem build() {
            return new TeklifKalem(id, teklifId, stokId, aciklama, miktar, birim, birimFiyat, iskontoOrani, kdvOrani, tutar);
        }
    }
}
