package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjandaOzetDTO {
    /** Bugünkü toplam olay sayısı (vade + proje görevi + kişisel görev + hatırlatıcı). */
    private long bugunOlay;
    /** Bu ay vadesi gelen ödenmemiş fatura kalan tutarı toplamı. */
    private BigDecimal buAyVadeToplam;
    /** Kullanıcının bekleyen kişisel görev sayısı. */
    private long bekleyenGorev;
    /** Bitiş tarihi geçmiş, hâlâ bekleyen kişisel görev sayısı. */
    private long gecikmisGorev;
}
