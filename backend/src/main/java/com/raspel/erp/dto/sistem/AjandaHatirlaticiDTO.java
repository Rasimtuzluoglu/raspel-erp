package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjandaHatirlaticiDTO {
    private Long id;
    @NotNull(message = "Gorev secilmelidir")
    private Long gorevId;
    @NotBlank(message = "Başlık zorunludur")
    @Size(max = 200, message = "Baslik en fazla 200 karakter olabilir")
    private String baslik;
    @NotNull(message = "Hatırlatma zamanı zorunludur")
    private LocalDateTime hatirlatmaZamani;
    private Boolean bildirildi;
    private LocalDateTime olusturmaTarihi;
}
