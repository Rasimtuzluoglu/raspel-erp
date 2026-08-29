package com.raspel.erp.dto.sistem;

import lombok.*;
import java.util.List;

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
    private Boolean sahaKullanici;
    private String token;

    private Boolean twoFactorGerekli;
    private String girisToken;
    private List<SirketDTO> sirketler;

    /** JWT'nin sona ereceği an (epoch ms). İstemci oturum sayacı için kullanılır; token'ın kendisi httpOnly cookie'dedir. */
    private Long tokenExpiresAt;
}