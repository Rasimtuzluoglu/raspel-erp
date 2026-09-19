package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorDTO {
    private Boolean enabled;
    private String secret;
    private String qrCodeUri;
    @Pattern(regexp = "^$|\\d{6}", message = "Dogrulama kodu 6 haneli olmalidir")
    private String code;
}