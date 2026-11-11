package com.raspel.erp.repository;

import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fatura listesi ve KPI sorgularını H2 üzerinde doğrular: filtreli özetin
 * liste filtresiyle aynı koşulları kullanması, tenant izolasyonu, arama
 * normalizasyonu ve sunucu taraflı sıralama.
 */
@SpringBootTest
@Transactional
class FaturaListeSorguH2Test {

    private static final Long SIRKET = 7711L;
    private static final Long BASKA_SIRKET = 7712L;
    private static final LocalDate BAS = LocalDate.of(2026, 11, 1);
    private static final LocalDate BIT = LocalDate.of(2026, 11, 30);
    private static final LocalDate TARIH = LocalDate.of(2026, 11, 15);
    private static final LocalDate BUGUN = LocalDate.of(2026, 11, 20);

    @Autowired private FaturaRepository faturaRepository;
    @Autowired private CariHesapRepository cariHesapRepository;

    @Test
    void ozetFiltreliListeIleAyniKosullariKullanir() {
        CariHesap acme = cari("Acme Ltd", SIRKET);
        CariHesap diger = cari("Diger Cari", SIRKET);
        fatura("H2-FL-1", SIRKET, acme, TARIH, Fatura.FaturaDurum.KESILDI, "1000", "400", "KISMI_ODENDI");
        fatura("H2-FL-2", SIRKET, acme, TARIH, Fatura.FaturaDurum.KESILDI, "500", "500", "ODENDI");
        // Filtre dışında kalanlar: farklı cari, taslak durum, başka şirket.
        fatura("H2-FL-3", SIRKET, diger, TARIH, Fatura.FaturaDurum.KESILDI, "900", "0", "ODENMEDI");
        fatura("H2-FL-4", SIRKET, acme, TARIH, Fatura.FaturaDurum.TASLAK, "700", "0", "ODENMEDI");
        fatura("H2-FL-5", BASKA_SIRKET, cari("Yabanci", BASKA_SIRKET), TARIH,
                Fatura.FaturaDurum.KESILDI, "300", "0", "ODENMEDI");

        Object[] ozet = faturaRepository.ozetFiltreli(SIRKET, "%acme%", BAS, BIT,
                Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI, "KISMI_ODENDI", null, null, BUGUN);

        Object[] satir = (ozet[0] instanceof Object[]) ? (Object[]) ozet[0] : ozet;
        assertEquals(1L, ((Number) satir[0]).longValue());
        assertEquals(0, new BigDecimal("1000").compareTo((BigDecimal) satir[1]));
        assertEquals(0, new BigDecimal("400").compareTo((BigDecimal) satir[2]));
        assertEquals(0, new BigDecimal("600").compareTo((BigDecimal) satir[3]));

        Page<Fatura> liste = faturaRepository.ara(SIRKET, "%acme%", BAS, BIT,
                Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI, "KISMI_ODENDI", null, null, BUGUN,
                PageRequest.of(0, 50, Sort.by(Sort.Order.desc("tarih"), Sort.Order.desc("id"))));

        assertEquals(1, liste.getTotalElements());
        assertEquals("H2-FL-1", liste.getContent().get(0).getFaturaNumarasi());
    }

    @Test
    void araSiralamaAlaninaGoreSayfalar() {
        CariHesap cari = cari("Sirali Cari", SIRKET);
        fatura("H2-FL-S1", SIRKET, cari, TARIH, Fatura.FaturaDurum.KESILDI, "100", "0", "ODENMEDI");
        fatura("H2-FL-S2", SIRKET, cari, TARIH, Fatura.FaturaDurum.KESILDI, "300", "0", "ODENMEDI");
        fatura("H2-FL-S3", SIRKET, cari, TARIH, Fatura.FaturaDurum.KESILDI, "200", "0", "ODENMEDI");

        Page<Fatura> artan = faturaRepository.ara(SIRKET, null, null, null, null, null, null, null, null, BUGUN,
                PageRequest.of(0, 2, Sort.by(Sort.Order.asc("genelToplam"))));

        assertEquals(3, artan.getTotalElements());
        assertEquals("H2-FL-S1", artan.getContent().get(0).getFaturaNumarasi());
        assertEquals("H2-FL-S3", artan.getContent().get(1).getFaturaNumarasi());

        Page<Fatura> azalan = faturaRepository.ara(SIRKET, null, null, null, null, null, null, null, null, BUGUN,
                PageRequest.of(0, 3, Sort.by(Sort.Order.desc("genelToplam"))));

        assertEquals("H2-FL-S2", azalan.getContent().get(0).getFaturaNumarasi());
    }

    @Test
    void cariAdinaGoreSiralamaIliskiUzerindenCalisir() {
        fatura("H2-FL-C1", SIRKET, cari("Zeta", SIRKET), TARIH, Fatura.FaturaDurum.KESILDI, "100", "0", "ODENMEDI");
        fatura("H2-FL-C2", SIRKET, cari("Alfa", SIRKET), TARIH, Fatura.FaturaDurum.KESILDI, "100", "0", "ODENMEDI");

        Page<Fatura> sayfa = faturaRepository.findBySirketId(SIRKET,
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("cariHesap.ad"))));

        assertEquals("Alfa", sayfa.getContent().get(0).getCariHesap().getAd());
        assertEquals(2, sayfa.getTotalElements());
    }

    private CariHesap cari(String ad, Long sirketId) {
        return cariHesapRepository.save(CariHesap.builder()
                .ad(ad).tur("Musteri").bakiye(BigDecimal.ZERO)
                .sirketId(sirketId).build());
    }

    private void fatura(String numara, Long sirketId, CariHesap cari, LocalDate tarih,
                        Fatura.FaturaDurum durum, String genelToplam, String odenen, String odemeDurumu) {
        faturaRepository.save(Fatura.builder()
                .faturaNumarasi(numara)
                .tarih(tarih)
                .vadeTarihi(tarih.plusDays(30))
                .tur(Fatura.FaturaTur.SATIS)
                .durum(durum)
                .odemeDurumu(odemeDurumu)
                // ara_toplam / kdv / genel_toplam nullable=false.
                .araToplam(new BigDecimal(genelToplam))
                .kdv(BigDecimal.ZERO)
                .genelToplam(new BigDecimal(genelToplam))
                .odenenTutar(new BigDecimal(odenen))
                .kalanTutar(new BigDecimal(genelToplam).subtract(new BigDecimal(odenen)))
                .cariHesap(cari)
                .sirketId(sirketId)
                .build());
    }
}
