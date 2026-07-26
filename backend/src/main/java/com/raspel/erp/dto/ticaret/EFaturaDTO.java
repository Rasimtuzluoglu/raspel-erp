package com.raspel.erp.dto.ticaret;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EFaturaDTO {
    private Long id;
    private Long faturaId;
    private String ettn;
    private String faturaNo;
    private String senaryo;
    private String tip;
    private Integer gibDurumKodu;
    private String gibDurumAciklama;
    private String aliciVknTckn;
    private String aliciUnvan;
    private BigDecimal odenecekTutar;
    private String ublXml;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}
