package com.raspel.erp.dto.sube;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "Kaynak depo secilmelidir")
    private Long kaynakDepoId;
    private String kaynakDepoAd;
    @NotNull(message = "Hedef depo seçilmelidir")
    private Long hedefDepoId;
    private String hedefDepoAd;
    @NotNull(message = "Stok secilmelidir")
    private Long stokId;
    private String stokAd;
    @NotNull(message = "Miktar zorunludur")
    @DecimalMin(value = "0.01", message = "Miktar sıfırdan büyük olmalıdır")
    private BigDecimal miktar;
    private String durum;
    private String aciklama;
    private Long olusturanKullaniciId;
    private LocalDateTime olusturmaTarihi;
    private LocalDateTime onayTarihi;
}
