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
}
