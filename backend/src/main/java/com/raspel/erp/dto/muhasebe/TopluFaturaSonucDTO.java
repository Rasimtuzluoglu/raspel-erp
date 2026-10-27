package com.raspel.erp.dto.muhasebe;

import lombok.*;

import java.util.List;

/** Toplu faturaya dönüştürme sonucu: başarılı ve atlanan kayıtlar. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopluFaturaSonucDTO {

    private List<Basarili> basarili;
    private List<Atlanan> atlanan;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Basarili {
        private Long irsaliyeId;
        private String irsaliyeNo;
        private Long faturaId;
        private String faturaNo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Atlanan {
        private Long irsaliyeId;
        private String irsaliyeNo;
        private String sebep;
    }
}
