package com.raspel.erp.dto.sistem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank(message = "Endpoint zorunludur")
    private String endpoint;
    @Valid
    @NotNull(message = "Anahtarlar zorunludur")
    private Keys keys;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Keys {
        @NotBlank(message = "p256dh anahtari zorunludur")
        private String p256dh;
        @NotBlank(message = "auth anahtari zorunludur")
        private String auth;
    }
}
