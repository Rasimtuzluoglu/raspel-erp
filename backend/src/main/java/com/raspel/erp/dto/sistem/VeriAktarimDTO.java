package com.raspel.erp.dto.sistem;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VeriAktarimDTO {
    private Long kaynakSirketId;
    private Long hedefSirketId;
    private boolean stoklariAktar;
    private boolean carileriAktar;
    private boolean bakiyeleriSifirla;
    private boolean fiyatlariKoru;
}
