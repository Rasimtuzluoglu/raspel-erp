package com.raspel.erp.dto.envanter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokDuzeltmeDTO {
    private Long id;
    private Long sirketId;
    @NotNull(message = "Stok secilmelidir")
    private Long stokId;
    private String stokAd;
    private BigDecimal eskiMiktar;
    @NotNull(message = "Yeni miktar zorunludur")
    @DecimalMin(value = "0.0", message = "Yeni miktar negatif olamaz")
    private BigDecimal yeniMiktar;
    @NotBlank(message = "Düzeltme nedeni zorunludur")
    @Size(max = 500, message = "Neden en fazla 500 karakter olabilir")
    private String neden;
    private Long kullaniciId;
    private LocalDateTime olusturmaTarihi;
}
