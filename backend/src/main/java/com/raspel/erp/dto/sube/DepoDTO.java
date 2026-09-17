package com.raspel.erp.dto.sube;

import lombok.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepoDTO {
    private Long id;
    @NotBlank(message = "Depo adı boş olamaz")
    private String ad;
    private String adres;
    private String yetkili;
    @NotNull(message = "Bağlı şube seçilmelidir")
    private Long subeId;
    private String subeAdi;
    private Long sirketId;
    private Boolean aktif;
    private LocalDateTime olusturmaTarihi;
}
