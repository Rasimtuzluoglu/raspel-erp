package com.raspel.erp.dto.sistem;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyKayitSecenekRequest {
    /** Kayit oncesi mevcut parola dogrulamasi. */
    private String password;
}
