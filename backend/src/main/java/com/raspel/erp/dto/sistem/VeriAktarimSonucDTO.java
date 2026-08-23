package com.raspel.erp.dto.sistem;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VeriAktarimSonucDTO {
    private int aktarilanStokSayisi;
    private int atlananStokSayisi;
    private int aktarilanCariSayisi;
    private int atlananCariSayisi;
    private String kaynakSirketAdi;
    private String hedefSirketAdi;
    private LocalDateTime aktarimTarihi;
}
