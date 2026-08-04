package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.raspel.erp.entity.finans.Hareket;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KasaHareketDTO {
    private Long id;
    private Long kasaId;
    private String kasaAd;
    @NotBlank(message = "Hareket türü seçilmelidir")
    private String tur;
    @NotNull(message = "Tutar girilmelidir")
    @DecimalMin(value = "0.01", message = "Tutar 0'dan büyük olmalıdır")
    private BigDecimal tutar;
    @NotNull(message = "Tarih girilmelidir")
    private LocalDate hareketTarihi;
    private String aciklama;
    private Long kategoriId;
    private String kategoriAd;
    private LocalDateTime olusturmaTarihi;
}