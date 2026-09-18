package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SifreSifirlaTalepRequest {

    @NotBlank(message = "Kullanıcı adı zorunludur")
    private String username;
}
