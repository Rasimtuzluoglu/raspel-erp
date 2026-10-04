package com.raspel.erp.dto.ticaret;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaturaKalemDTO {

    private Long id;

    @NotBlank(message = "Kalem açıklaması girilmelidir")
    private String aciklama;

    @NotNull(message = "Adet girilmelidir")
    @DecimalMin(value = "0.01", message = "Adet 0'dan büyük olmalıdır")
    private BigDecimal adet;

    @NotNull(message = "Birim fiyat girilmelidir")
    @DecimalMin(value = "0.00", message = "Birim fiyat negatif olamaz")
    @Digits(integer = 17, fraction = 2, message = "Birim fiyat en fazla 2 ondalık basamak ve 17 haneli olabilir")
    private BigDecimal birimFiyat;

    // REDTEAM C1: Bu iki alan doğrulamasızdı. Negatif iskonto oranı ile
    // (örn. -500) 1 adet 100 TL'lik stok 600 TL'lik faturaya dönüşüyor ve
    // kasa kaydı gerçek nakit YARATIYORDU. Sınırlar hem burada hem de
    // FaturaTutar.satir() içinde uygulanır (savunma derinliği).
    @DecimalMin(value = "0", message = "KDV oranı negatif olamaz")
    @DecimalMax(value = "100", message = "KDV oranı %100'den büyük olamaz")
    @Digits(integer = 5, fraction = 2, message = "KDV oranı en fazla 2 ondalık basamak olabilir")
    private BigDecimal kdvOrani;

    @DecimalMin(value = "0", message = "İskonto oranı negatif olamaz")
    @DecimalMax(value = "100", message = "İskonto oranı %100'den büyük olamaz")
    @Digits(integer = 5, fraction = 2, message = "İskonto oranı en fazla 2 ondalık basamak olabilir")
    private BigDecimal iskontoOrani;

    private BigDecimal tutar;

    private Long stokId;
    private String stokAd;
    private String stokKodu;
    private BigDecimal agirlik;
}