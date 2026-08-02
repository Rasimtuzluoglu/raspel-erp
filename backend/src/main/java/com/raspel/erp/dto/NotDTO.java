package com.raspel.erp.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotDTO {
    private Long id;

    @NotBlank(message = "Başlık boş olamaz")
    @Size(max = 200)
    private String baslik;

    private String icerik;

    private String onemDerecesi;

    private String renk;

    private Long kullaniciId;

    private LocalDateTime olusturmaTarihi;

    private LocalDateTime guncellemeTarihi;
}
