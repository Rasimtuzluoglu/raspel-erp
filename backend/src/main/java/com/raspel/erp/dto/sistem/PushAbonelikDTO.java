package com.raspel.erp.dto.sistem;

import lombok.*;

/**
 * Tarayici PushSubscription nesnesinin backend karsiligi.
 * JSON: { "endpoint": "...", "keys": { "p256dh": "...", "auth": "..." } }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushAbonelikDTO {

    private String endpoint;
    private Keys keys;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Keys {
        private String p256dh;
        private String auth;
    }
}
