package com.raspel.erp.dto.sistem;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjandaHatirlaticiDTO {
    private Long id;
    private Long gorevId;
    private String baslik;
    private LocalDateTime hatirlatmaZamani;
    private Boolean bildirildi;
    private LocalDateTime olusturmaTarihi;
}
