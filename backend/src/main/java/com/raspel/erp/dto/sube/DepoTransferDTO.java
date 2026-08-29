package com.raspel.erp.dto.sube;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepoTransferDTO {
    private Long id;
    private Long sirketId;
    private Long kaynakDepoId;
    private String kaynakDepoAd;
    private Long hedefDepoId;
    private String hedefDepoAd;
    private Long stokId;
    private String stokAd;
    private BigDecimal miktar;
    private String durum;
    private String aciklama;
    private Long olusturanKullaniciId;
    private LocalDateTime olusturmaTarihi;
    private LocalDateTime onayTarihi;
}
