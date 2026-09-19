package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariFiyatDTO {
    private Long id;
    @NotNull(message = "Cari hesap secilmelidir")
    private Long cariHesapId;
    @NotNull(message = "Stok secilmelidir")
    private Long stokId;
    private String stokAd;
    private String stokKodu;
    @NotNull(message = "Fiyat zorunludur")
    @DecimalMin(value = "0.0", message = "Fiyat negatif olamaz")
    private BigDecimal fiyat;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}
