package com.raspel.erp.dto.envanter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimEmriDTO {
    private Long id;
    private Long sirketId;
    private Long siparisId;
    @NotNull(message = "Üretilecek ürün secilmelidir")
    private Long urunId;
    private String urunAd;
    @NotNull(message = "Miktar zorunludur")
    @DecimalMin(value = "0.01", message = "Miktar sıfırdan büyük olmalıdır")
    private BigDecimal miktar;
    private String durum;
    private String aciklama;
    private LocalDateTime olusturmaTarihi;
    private LocalDateTime tamamlanmaTarihi;
    private LocalDate planlananBaslangic;
    private LocalDate planlananBitis;
    private LocalDateTime baslamaTarihi;
    private String oncelik;
    private BigDecimal uretilenMiktar;
    private BigDecimal fireMiktar;
    private Long sorumluPersonelId;
    private String sorumluPersonelAd;
    private Long depoId;
    private String depoAd;
    private BigDecimal hammaddeMaliyeti;
    private BigDecimal iscilikMaliyeti;
    private BigDecimal toplamMaliyet;
    private String notlar;
    private boolean gecikti;
}
