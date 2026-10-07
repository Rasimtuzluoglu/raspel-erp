package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Faz 2.3: Cari hesaplarda toplu alan güncelleme. Yalnızca gönderilen (null
 * olmayan) alanlar uygulanır; gönderilmeyen alanlar değiştirilmez.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariTopluGuncelleDTO {

    @NotEmpty(message = "Güncellenecek kayıt seçilmedi")
    private List<Long> idler;

    @Pattern(regexp = "(?i)(musteri|tedarikci|her ikisi|diger)",
            message = "Geçersiz cari türü (Musteri/Tedarikci/Her Ikisi/Diger)")
    private String tur;

    private Long temsilciId;
    private String temsilciAd;

    @DecimalMin(value = "0", message = "Kredi limiti negatif olamaz")
    @Digits(integer = 17, fraction = 2, message = "Kredi limiti en fazla 2 ondalık basamak olabilir")
    private BigDecimal krediLimiti;

    @Min(value = 0, message = "Ödeme vadesi negatif olamaz")
    private Integer odemeVadesi;

    private Boolean aktif;
}
