package com.raspel.erp.dto.ticaret;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeslimatDTO {
    private Long id;
    private Long sirketId;
    private Long faturaId;
    private String faturaNumarasi;
    private Long driverId;
    private String driverAd;
    private String teslimatAdresi;
    private LocalDate beklenenTeslimTarihi;
    private String teslimatFoto;
    private String musteriAdi;
    private String durum;
    private String notlar;
    private LocalDateTime olusturmaTarihi;
    private LocalDateTime teslimTarihi;
    private boolean gecikti;
}
