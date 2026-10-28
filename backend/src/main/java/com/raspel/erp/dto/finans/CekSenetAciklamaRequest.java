package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.Size;
import lombok.*;

/** Teminat / protesto gibi serbest açıklamalı işlemlerin gövdesi. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CekSenetAciklamaRequest {
    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
    private String aciklama;
}
