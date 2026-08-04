package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KategoriDTO {
    private Long id;
    @NotBlank(message = "Kategori adı boş olamaz")
    private String ad;
    @NotBlank(message = "Tür seçilmelidir")
    private String tur;
    private LocalDateTime olusturmaTarihi;
}