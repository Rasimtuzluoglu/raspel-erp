package com.raspel.erp.dto.sistem;

import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeySecenekDTO {
    private String challengeId;
    private Map<String, Object> publicKey;
}
