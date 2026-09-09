package com.raspel.erp.dto.ticaret;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeslimatDurumLogDTO {
    private Long id;
    private Long teslimatId;
    private String oncekiDurum;
    private String yeniDurum;
    private Long kullaniciId;
    private LocalDateTime olusturmaTarihi;
}
