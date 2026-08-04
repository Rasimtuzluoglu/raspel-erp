package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorGirisRequest {
    @NotBlank(message = "Giriş token'ı eksik")
    private String girisToken;

    @NotBlank(message = "Doğrulama kodu girilmelidir")
    private String code;

    private String companyName;
    private Long sirketId;
}