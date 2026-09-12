package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaksitOdeDTO {
    /** Odemenin yapildigi tarih (bos ise bugun). */
    private LocalDate odemeTarihi;

    /** Taksiti kapatan tahsilat hareketi (opsiyonel). */
    private Long hareketId;

    @Size(max = 500, message = "Aciklama en fazla 500 karakter olabilir")
    private String aciklama;
}
