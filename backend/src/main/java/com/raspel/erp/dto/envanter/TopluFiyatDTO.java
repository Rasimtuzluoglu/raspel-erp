package com.raspel.erp.dto.envanter;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopluFiyatDTO {
    private String kategori;
    private String stokGrubu;
    private String marka;

    @NotBlank(message = "Yön seçilmelidir (ARTIR/AZALT)")
    private String yon;

    @NotNull(message = "Oran girilmelidir")
    @DecimalMin(value = "0.1", message = "Oran 0.1'den küçük olamaz")
    @DecimalMax(value = "1000", message = "Oran 1000'den büyük olamaz")
    private Double oran;
}
