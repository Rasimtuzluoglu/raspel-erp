package com.raspel.erp.repository;

import com.raspel.erp.dto.finans.TahsilatIslemDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.finans.TahsilatIslem;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.finans.TahsilatIslemRepository;
import com.raspel.erp.service.finans.TahsilatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V170 modelinin gerçek sorgularla doğrulanması:
 * <ul>
 *   <li>{@code @SQLRestriction("iptal = false")} iptal edilmiş hareketi normal
 *       sorgularda gizler, ancak native dağıtım sorgusu geçmiş için döndürür.</li>
 *   <li>Geçmiş filtreleri (Specification + cari arama alt sorgusu) H2 üzerinde
 *       derlenip çalışır.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TahsilatIslemRepositoryTest {

    private static final Long SIRKET = 9701L;

    @Autowired private TahsilatIslemRepository tahsilatIslemRepository;
    @Autowired private HareketRepository hareketRepository;
    @Autowired private CariHesapRepository cariHesapRepository;
    @Autowired private TahsilatService tahsilatService;

    private CariHesap cari;

    @BeforeEach
    void setUp() {
        hareketRepository.deleteAll();
        tahsilatIslemRepository.deleteAll();
        cari = cariHesapRepository.save(CariHesap.builder()
                .ad("Alpha Müşteri").telefon("05551112233")
                .bakiye(BigDecimal.ZERO).sirketId(SIRKET).build());
    }

    private TahsilatIslem islem(String durum, LocalDate tarih, Long legacyHareketId) {
        return tahsilatIslemRepository.save(TahsilatIslem.builder()
                .sirketId(SIRKET)
                .cariId(cari.getId())
                .tarih(tarih)
                .tutar(new BigDecimal("600.00"))
                .odemeYontemi("NAKIT")
                .durum(durum)
                .legacyHareketId(legacyHareketId)
                .build());
    }

    private Hareket hareket(TahsilatIslem islem, String tutar, boolean iptal) {
        return hareketRepository.save(Hareket.builder()
                .cariHesap(cari)
                .tur(Hareket.HareketTuru.TAHSILAT)
                .tutar(new BigDecimal(tutar))
                .hareketTarihi(islem.getTarih())
                .aciklama("Tahsilat")
                .tahsilatIslemId(islem.getId())
                .sirketId(SIRKET)
                .iptal(iptal)
                .build());
    }

    @Test
    void dagilimSorgusuIptalEdilmisHareketiDeDoner() {
        TahsilatIslem islem = islem(TahsilatIslem.DURUM_AKTIF, LocalDate.now().minusDays(1), null);
        Hareket aktif = hareket(islem, "500.00", false);
        Hareket iptalli = hareket(islem, "100.00", true);

        List<Object[]> satirlar = hareketRepository.tahsilatDagilimlariniGetir(List.of(islem.getId()));

        assertEquals(2, satirlar.size());
        // Normal JPQL @SQLRestriction uygular: yalnız iptal olmayan hareket.
        assertEquals(List.of(aktif.getId()), hareketRepository.findIdByTahsilatIslemId(islem.getId()));
        assertTrue(satirlar.stream().anyMatch(r -> ((Number) r[1]).longValue() == iptalli.getId()));
    }

    @Test
    void legacyIslemGecmisteTekSatirOlarakDoner() {
        TahsilatIslem legacy = islem(TahsilatIslem.DURUM_AKTIF, LocalDate.now().minusMonths(3), 4242L);
        hareket(legacy, "600.00", false);

        Page<TahsilatIslemDTO> sayfa = tahsilatService.gecmisFiltreli(
                SIRKET, "alpha", null, "NAKIT", "AKTIF", null, null, null, null,
                PageRequest.of(0, 25));

        assertEquals(1, sayfa.getTotalElements());
        TahsilatIslemDTO dto = sayfa.getContent().get(0);
        assertEquals(legacy.getId(), dto.getId());
        assertEquals("Alpha Müşteri", dto.getCariAd());
        assertEquals(1, dto.getSatirSayisi());
        assertEquals(4242L, legacy.getLegacyHareketId());
    }

    @Test
    void gecmisDurumVeCariFiltreleriUygulanir() {
        TahsilatIslem aktif = islem(TahsilatIslem.DURUM_AKTIF, LocalDate.now(), null);
        hareket(aktif, "600.00", false);
        TahsilatIslem iptal = islem(TahsilatIslem.DURUM_IPTAL, LocalDate.now().minusDays(2), null);
        hareket(iptal, "600.00", false);

        Page<TahsilatIslemDTO> iptaller = tahsilatService.gecmisFiltreli(
                SIRKET, null, cari.getId(), null, "IPTAL", null, null, null, null,
                PageRequest.of(0, 25));
        Page<TahsilatIslemDTO> baskaCari = tahsilatService.gecmisFiltreli(
                SIRKET, null, -1L, null, null, null, null, null, null,
                PageRequest.of(0, 25));

        assertEquals(1, iptaller.getTotalElements());
        assertEquals(TahsilatIslem.DURUM_IPTAL, iptaller.getContent().get(0).getDurum());
        assertEquals(0, baskaCari.getTotalElements());
    }

    @Test
    void legacyHareketEslestirmesiSorgulanir() {
        TahsilatIslem legacy = islem(TahsilatIslem.DURUM_AKTIF, LocalDate.now(), 5555L);

        assertTrue(tahsilatIslemRepository.existsByLegacyHareketId(5555L));
        assertEquals(legacy.getId(),
                tahsilatIslemRepository.findByLegacyHareketId(5555L).orElseThrow().getId());
    }
}
