package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariHesapDTO {

    private Long id;

    @NotBlank(message = "Cari adı boş olamaz")
    @Size(max = 255, message = "Cari adı en fazla 255 karakter olabilir")
    private String ad;

    @Size(max = 50, message = "Vergi numarası en fazla 50 karakter olabilir")
    private String vergiNumarasi;

    @Size(max = 20, message = "Telefon en fazla 20 karakter olabilir")
    @Pattern(regexp = "^[0-9+()\\s-]*$", message = "Telefon yalnızca rakam, boşluk ve +()- içerebilir")
    private String telefon;

    @Size(max = 100, message = "E-posta en fazla 100 karakter olabilir")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    private String email;

    @Size(max = 500, message = "Adres en fazla 500 karakter olabilir")
    private String adres;

    @Pattern(regexp = "(?i)(musteri|tedarikci|her ikisi|diger)",
            message = "Geçersiz cari türü (Musteri/Tedarikci/Her Ikisi/Diger)")
    private String tur;
    private String il;
    private String ilce;
    private String vergiDairesi;
    private String yetkiliKisi;
    private String yetkiliTelefon;
    private String iban;
    private String notlar;
    /** Virgülle ayrık etiketler (ör. "vip, bayilik"). */
    @Size(max = 500, message = "Etiketler en fazla 500 karakter olabilir")
    private String etiketler;
    private String fotoUrl;
    /** Küçük (thumbnail) görsel adresi; liste/kart görünümleri bunu kullanır. */
    private String fotoThumbUrl;
    private Boolean aktif;

    @Pattern(regexp = "(?i)TRY|USD|EUR|GBP", message = "Geçersiz para birimi (TRY/USD/EUR/GBP)")
    private String paraBirimi;

    @DecimalMin(value = "0", message = "Kredi limiti negatif olamaz")
    @Digits(integer = 17, fraction = 2, message = "Kredi limiti en fazla 2 ondalık basamak olabilir")
    private BigDecimal krediLimiti;
    @Min(value = 0, message = "Ödeme vadesi negatif olamaz")
    private Integer odemeVadesi;
    @Digits(integer = 17, fraction = 2, message = "Bakiye en fazla 2 ondalık basamak olabilir")
    private BigDecimal bakiye;
    private Long temsilciId;
    private String temsilciAd;
    private LocalDateTime olusturmaTarihi;
    private LocalDateTime guncellemeTarihi;
}