package com.raspel.erp.dto.muhasebe;

import lombok.*;

/** İptal geçişi gövdesi. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrsaliyeIptalIstek {
    /** Opsiyonel iptal açıklaması (durum loguna yazılır). */
    private String aciklama;
}
