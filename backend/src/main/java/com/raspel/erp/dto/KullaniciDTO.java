package com.raspel.erp.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KullaniciDTO {
    private Long id;
    @NotBlank(message = "Kullanıcı adı boş olamaz")
    private String username;
    private String password;
    @NotBlank(message = "Görünen ad boş olamaz")
    private String displayName;
    private String avatarUrl;
    private String companyName;
    private Long sirketId;
    private String role;
    private Boolean active;
    private Boolean twoFactorEnabled;
    private LocalDateTime olusturmaTarihi;
}
