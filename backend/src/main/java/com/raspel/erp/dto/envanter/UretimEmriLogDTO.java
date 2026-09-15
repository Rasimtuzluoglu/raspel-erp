package com.raspel.erp.dto.envanter;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimEmriLogDTO {
    private Long id;
    private Long uretimEmriId;
    private String oncekiDurum;
    private String yeniDurum;
    private Long kullaniciId;
    private String aciklama;
    private LocalDateTime olusturmaTarihi;
}
