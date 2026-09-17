package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdresDefteriDTO {
    private Long id;

    @NotBlank(message = "Ad/unvan boş olamaz")
    @Size(max = 200, message = "Ad/unvan en fazla 200 karakter olabilir")
    private String ad;

    @Size(max = 50)
    private String tur;

    @Size(max = 30)
    private String telefon;

    @Email(message = "Geçerli bir e-posta giriniz")
    @Size(max = 150)
    private String email;

    @Size(max = 500)
    private String adres;

    @Size(max = 500)
    private String etiketler;

    private String notlar;

    private LocalDateTime olusturmaTarihi;
    private LocalDateTime guncellemeTarihi;
}
