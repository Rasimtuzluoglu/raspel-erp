package com.raspel.erp.dto.sistem;

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
    private String code;
}