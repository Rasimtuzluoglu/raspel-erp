package com.raspel.erp.dto.sistem;

import lombok.*;

/**
 * Sabit sistem kategorileri (personel masraf kodları). Salt-okunurdur;
 * registry'ye yazılmaz ve personel masraf akışı bu kodlarla çalışır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SistemKategoriDTO {
    private String kod;
    private String ad;
    private String kapsam;
}
