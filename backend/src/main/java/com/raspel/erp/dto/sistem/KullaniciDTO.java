package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

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
    private List<Long> sirketIds;
    private String role;
    private Boolean active;
    private Boolean twoFactorEnabled;
    private Boolean sahaKullanici;
    private LocalDateTime olusturmaTarihi;
}