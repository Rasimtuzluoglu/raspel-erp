package com.raspel.erp.dto.ticaret;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurucuDTO {
    private Long id;
    private String ad;
    private Long bekleyenTeslimatSayisi;
    /** Kullanicinin rolu; DRIVER degilse sofor ekranini goremez (uyari icin). */
    private String rol;
}
