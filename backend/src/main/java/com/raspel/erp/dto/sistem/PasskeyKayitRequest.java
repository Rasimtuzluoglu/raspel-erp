package com.raspel.erp.dto.sistem;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyKayitRequest {
    private String challengeId;
    private String etiket;
    /** navigator.credentials.create() sonucu (PublicKeyCredential JSON). */
    private java.util.Map<String, Object> credential;
}
