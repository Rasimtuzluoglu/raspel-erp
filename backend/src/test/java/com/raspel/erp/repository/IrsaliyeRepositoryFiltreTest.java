package com.raspel.erp.repository;

import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.muhasebe.Irsaliye;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link IrsaliyeRepository} filtre/özet sorgularının H2 üzerinde gerçekten
 * çalıştığını doğrular (JPQL ayrıştırması bağlam yüklenirken geçse bile
 * {@code :faturali = true} gibi boolean karşılaştırmaları çalışma anında
 * patlayabilir).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IrsaliyeRepositoryFiltreTest {

    @Autowired
    private IrsaliyeRepository irsaliyeRepository;

    @Autowired
    private CariHesapRepository cariHesapRepository;

    private static final Long SIRKET = 9001L;
    private static final PageRequest PAGE = PageRequest.of(0, 50,
            Sort.by(Sort.Direction.DESC, "tarih"));

    private CariHesap musteriA;
    private CariHesap musteriB;

    @BeforeEach
    void setUp() {
        irsaliyeRepository.deleteAll();
        musteriA = cariHesapRepository.save(CariHesap.builder()
                .ad("Musteri A").sirketId(SIRKET).bakiye(BigDecimal.ZERO).build());
        musteriB = cariHesapRepository.save(CariHesap.builder()
                .ad("Musteri B").sirketId(SIRKET).bakiye(BigDecimal.ZERO).build());

        irsaliyeRepository.save(irsaliye("IRS-100", "TASLAK", "SATIS", musteriA.getId(), null, LocalDate.now()));
        irsaliyeRepository.save(irsaliye("IRS-200", "SEVK_EDILDI", "SATIS", musteriA.getId(), 5L, LocalDate.now().minusDays(1)));
        irsaliyeRepository.save(irsaliye("IRS-300", "IPTAL", "SATIS", musteriB.getId(), 6L, LocalDate.now().minusDays(2)));
        Irsaliye teslim = irsaliye("IRS-400", "TESLIM_EDILDI", "ALIS", musteriB.getId(), 7L, LocalDate.now().minusDays(3));
        teslim.setTeslimTarihi(LocalDateTime.now());
        irsaliyeRepository.save(teslim);
        // Başka şirket: tenant filtresi dışında kalmalı.
        Irsaliye diger = irsaliye("IRS-500", "TASLAK", "SATIS", musteriA.getId(), null, LocalDate.now());
        diger.setSirketId(9002L);
        irsaliyeRepository.save(diger);
    }

    private Irsaliye irsaliye(String no, String durum, String tur, Long cariId, Long faturaId, LocalDate tarih) {
        return Irsaliye.builder()
                .irsaliyeNo(no).durum(durum).tur(tur).cariHesapId(cariId)
                .faturaId(faturaId).tarih(tarih).sirketId(SIRKET)
                .build();
    }

    @Test
    void aramaIrsaliyeNoVeCariAdindaCalisir() {
        // Not: Türkçe yerel ayarda H2'nin LOWER('I') -> 'ı' üretmesi nedeniyle
        // desende büyük "I" harfi kullanılmaz (uygulama genelinde aynı kısıt var).
        var noIle = irsaliyeRepository.filtreliGetir(SIRKET, "%200%", null, null, null,
                null, null, null, PAGE);
        assertEquals(1, noIle.getTotalElements());
        assertEquals("IRS-200", noIle.getContent().get(0).getIrsaliyeNo());

        var cariIle = irsaliyeRepository.filtreliGetir(SIRKET, "%musteri a%", null, null, null,
                null, null, null, PAGE);
        assertEquals(2, cariIle.getTotalElements());
    }

    @Test
    void durumTurCariVeFaturaliFiltreleriBirlikteCalisir() {
        assertEquals(1, irsaliyeRepository.filtreliGetir(SIRKET, null, "TASLAK", null, null,
                null, null, null, PAGE).getTotalElements());
        assertEquals(1, irsaliyeRepository.filtreliGetir(SIRKET, null, null, "ALIS", null,
                null, null, null, PAGE).getTotalElements());
        assertEquals(2, irsaliyeRepository.filtreliGetir(SIRKET, null, null, null, musteriB.getId(),
                null, null, null, PAGE).getTotalElements());
        assertEquals(1, irsaliyeRepository.filtreliGetir(SIRKET, null, null, null, null,
                null, null, Boolean.FALSE, PAGE).getTotalElements());
        assertEquals(3, irsaliyeRepository.filtreliGetir(SIRKET, null, null, null, null,
                null, null, Boolean.TRUE, PAGE).getTotalElements());
    }

    @Test
    void tarihAraligiFiltresiCalisir() {
        var sonuc = irsaliyeRepository.filtreliGetir(SIRKET, null, null, null, null,
                LocalDate.now().minusDays(1), LocalDate.now(), null, PAGE);
        assertEquals(2, sonuc.getTotalElements());
    }

    @Test
    void ozetSayimlariTenantVeDurumaGoreDoner() {
        assertEquals(1, irsaliyeRepository.countDurum(SIRKET, "TASLAK", null, null));
        assertEquals(1, irsaliyeRepository.countDurum(SIRKET, "SEVK_EDILDI", null, null));
        assertEquals(1, irsaliyeRepository.countDurum(SIRKET, "IPTAL", null, null));
        assertEquals(1, irsaliyeRepository.countFaturasiz(SIRKET, null, null));
        assertEquals(1, irsaliyeRepository.countTeslimEdilen(SIRKET,
                LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay()));
    }

    @Test
    void siralamaTarihAzalan() {
        List<Irsaliye> icerik = irsaliyeRepository.filtreliGetir(SIRKET, null, null, null, null,
                null, null, null, PAGE).getContent();
        assertEquals("IRS-100", icerik.get(0).getIrsaliyeNo());
        assertEquals("IRS-400", icerik.get(icerik.size() - 1).getIrsaliyeNo());
    }
}
