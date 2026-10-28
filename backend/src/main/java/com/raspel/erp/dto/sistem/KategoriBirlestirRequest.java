package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/** İki kategoriyi birleştirme isteği. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KategoriBirlestirRequest {

    @NotNull(message = "Kaynak kategori seçilmelidir")
    private Long kaynakId;

    @NotNull(message = "Hedef kategori seçilmelidir")
    private Long hedefId;
}
