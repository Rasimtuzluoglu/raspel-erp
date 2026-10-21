package com.raspel.erp.dto.ik;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IzinOzetDTO {
    private long bekleyen;
    private long onaylandi;
    private long reddedildi;
    /** Bugün onaylı izni olan personel sayısı. */
    private long bugunIzinli;
}
