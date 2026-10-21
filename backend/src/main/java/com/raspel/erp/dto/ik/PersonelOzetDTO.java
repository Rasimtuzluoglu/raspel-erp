package com.raspel.erp.dto.ik;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonelOzetDTO {
    private long toplam;
    private long aktif;
    private long pasif;
    /** Şirket genelinde onay bekleyen izin talebi sayısı. */
    private long bekleyenIzin;
}
