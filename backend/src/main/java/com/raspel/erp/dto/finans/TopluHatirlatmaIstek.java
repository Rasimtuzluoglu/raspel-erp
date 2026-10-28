package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

/** Toplu e-posta hatırlatma isteği. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopluHatirlatmaIstek {

    @NotEmpty(message = "En az bir cari seçilmelidir")
    private List<Long> cariIds;
}
