package com.raspel.erp.dto.sistem;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 2 seviyeli kategori ağacı düğümü: tüm {@link KategoriDTO} alanları +
 * alt kategoriler. Alt kategorilerin {@code altKategoriler} listesi boştur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class KategoriAgacDTO extends KategoriDTO {

    @Builder.Default
    private List<KategoriAgacDTO> altKategoriler = new ArrayList<>();
}
