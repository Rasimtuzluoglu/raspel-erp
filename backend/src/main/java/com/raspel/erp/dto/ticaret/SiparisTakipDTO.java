package com.raspel.erp.dto.ticaret;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiparisTakipDTO {
    private Long siparisId;
    private String siparisNo;
    private String cariAd;
    private String siparisDurum;
    private String uretimDurum;
    private long uretimSayisi;
    private String sevkDurum;
    private long sevkSayisi;
    private String teslimatDurum;
    private long teslimatSayisi;
}
