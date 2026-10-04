package com.raspel.erp.dto.envanter;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.raspel.erp.entity.finans.Hareket;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StokHareketDTO {
    private Long id;
    private Long stokId;
    private String stokAd;
    private String stokKodu;
    @NotBlank(message = "Hareket türü seçilmelidir")
    private String tur;

    /**
     * REDTEAM C3: {@code @NotNull} tek başına yetmiyordu. {@code miktar=-5000}
     * gönderildiğinde "yetersiz stok" kontrolü geçiyor ve {@code GIRIS} dalında
     * stok miktarı <b>artıyordu</b> (100 → 5100). Miktar daima pozitif olmalıdır;
     * yön bilgisi zaten {@code tur} alanında taşınır.
     */
    @NotNull(message = "Miktar girilmelidir")
    @DecimalMin(value = "0.000001", message = "Miktar pozitif olmalıdır")
    @Digits(integer = 17, fraction = 4, message = "Miktar en fazla 4 ondalık basamak olabilir")
    private BigDecimal miktar;
    @NotNull(message = "Tarih girilmelidir")
    private LocalDate hareketTarihi;
    private String aciklama;
    private Long cariHesapId;
    private String cariHesapAd;
    private Long depoId;
    private String depoAd;
    private String kaynakTip;
    private Long kaynakId;
    private Long seriId;
    private String seriNo;
    private BigDecimal agirlik;
    private LocalDateTime olusturmaTarihi;
}