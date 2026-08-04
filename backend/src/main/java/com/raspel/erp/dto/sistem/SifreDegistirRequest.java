package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SifreDegistirRequest {
    @NotBlank(message = "Mevcut şifre boş olamaz")
    private String mevcutSifre;

    @NotBlank(message = "Yeni şifre boş olamaz")
    private String yeniSifre;
}