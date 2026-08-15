package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * İlk kurulum isteği.
 * Sistem boşken (hiç firma yokken) firma bilgileri + yönetici hesabı ile kurulumu başlatır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KurulumDTO {

    @NotBlank(message = "Firma adı zorunludur")
    private String ad;

    @NotBlank(message = "Vergi numarası zorunludur")
    private String vergiNo;

    private String vergiDairesi;
    private String adres;
    private String telefon;
    private String email;
    private String webSite;

    @NotBlank(message = "Kullanıcı adı zorunludur")
    private String adminUsername;

    @NotBlank(message = "Şifre zorunludur")
    private String adminPassword;

    private String adminDisplayName;
}
