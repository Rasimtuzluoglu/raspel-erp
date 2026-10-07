package com.raspel.erp.dto.sistem;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SohbetOdaUyeDTO {
    private Long kullaniciId;
    private String kullaniciAd;
    /** Faz 3.2: OWNER / ADMIN / MEMBER. */
    private String rol;
}
