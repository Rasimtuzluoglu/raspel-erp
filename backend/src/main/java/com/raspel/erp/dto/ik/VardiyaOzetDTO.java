package com.raspel.erp.dto.ik;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VardiyaOzetDTO {
    private long toplam;
    private long bugun;
    private long buHafta;
    /** Vardiyası olan farklı personel sayısı. */
    private long personelSayisi;
}
