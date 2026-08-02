package com.raspel.erp.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private Long sirketId;
    private String sirketAdi;
    private String companyName;
    private String role;
    private String token;

    private Boolean twoFactorGerekli;
    private String girisToken;
}
