package com.raspel.erp.dto.envanter;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceteKalemDTO {
    private Long id;
    private Long receteId;

    @NotNull(message = "Hammadde seçilmelidir")
    private Long hammaddeId;

    private String hammaddeAd;

    /**
     * REDTEAM C4: Doğrulama hiç yoktu. Negatif miktar (örn. -10) ile üretim
     * tamamlandığında {@code gereken} negatif hesaplanıyor, "yetersiz hammadde"
     * kontrolü geçiyor ve hammadde stoğu <b>ARTIYORDU</b> (menzil/birim maliyet
     * negatifleşiyordu). Tüketim miktarı daima pozitif olmalıdır.
     */
    @NotNull(message = "Miktar girilmelidir")
    @DecimalMin(value = "0.000001", message = "Miktar pozitif olmalıdır")
    @Digits(integer = 17, fraction = 4, message = "Miktar en fazla 4 ondalık basamak olabilir")
    private BigDecimal miktar;

    private String birim;

    /** Fire (kayıp) oranı: %0-100. Üstü anlamsız, negatif kayıp olamaz. */
    @DecimalMin(value = "0", message = "Fire oranı negatif olamaz")
    @DecimalMax(value = "100", message = "Fire oranı %100'den büyük olamaz")
    @Digits(integer = 5, fraction = 2, message = "Fire oranı en fazla 2 ondalık basamak olabilir")
    private BigDecimal fireOrani;
}