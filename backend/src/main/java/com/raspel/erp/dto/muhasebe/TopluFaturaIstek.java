package com.raspel.erp.dto.muhasebe;

import lombok.*;

import java.util.List;

/** Toplu faturaya dönüştürme isteği. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopluFaturaIstek {
    private List<Long> ids;
}
