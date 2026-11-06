package com.raspel.erp.dto.sistem;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyGirisRequest {
    private String challengeId;
    /** navigator.credentials.get() sonucu (PublicKeyCredential JSON). */
    private java.util.Map<String, Object> credential;
    private String sirketKodu;
    private Boolean rememberMe;
}
