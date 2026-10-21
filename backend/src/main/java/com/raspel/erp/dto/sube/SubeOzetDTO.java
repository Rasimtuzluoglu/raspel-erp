package com.raspel.erp.dto.sube;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubeOzetDTO {
    private long toplam;
    private long aktif;
    private long pasif;
    /** Şirket genelindeki toplam depo sayısı. */
    private long depo;
}
