package com.raspel.erp.dto.envanter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokFiyatDTO {
    private Long id;
    @NotNull(message = "Stok secilmelidir")
    private Long stokId;
    @Size(max = 200, message = "Fiyat adı en fazla 200 karakter olabilir")
    private String ad;
    @NotNull(message = "Fiyat zorunludur")
    @DecimalMin(value = "0.0", message = "Fiyat negatif olamaz")
    private BigDecimal fiyat;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}
