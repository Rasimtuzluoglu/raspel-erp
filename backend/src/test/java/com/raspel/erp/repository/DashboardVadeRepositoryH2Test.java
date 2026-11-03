package com.raspel.erp.repository;

import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.VadeTakvimProjeksiyon;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vade takvimi JPQL sorgusunu H2 üzerinde doğrular: tenant izolasyonu,
 * KESILDI/ödeme kapanmamış filtresi, tarih aralığı, vade tarihine göre artan
 * sıralama ve projeksiyon eşlemesi.
 */
@SpringBootTest
@Transactional
class DashboardVadeRepositoryH2Test {

    private static final Long SIRKET = 993L;
    private static final Long BASKA_SIRKET = 994L;
    private static final LocalDate BAS = LocalDate.of(2026, 11, 1);
    private static final LocalDate BIT = LocalDate.of(2026, 11, 30);
    private static final List<String> KAPALI = List.of("ODENDI", "IPTAL");

    @Autowired private FaturaRepository faturaRepository;
    @Autowired private CariHesapRepository cariHesapRepository;

    @Test
    void vadeTakvimiSuzerVeVadeTarihineGoreSiralar() {
        CariHesap c = cari("Vade Cari", SIRKET);
        fatura("H2-VD-1", SIRKET, c, BAS.plusDays(19), "1000", "400", "ODENMEDI", Fatura.FaturaDurum.KESILDI);
        fatura("H2-VD-2", SIRKET, c, BAS.plusDays(4), "500", "0", "KISMI", Fatura.FaturaDurum.KESILDI);

        // Hariç tutulması gerekenler: ödenmiş, iptal, aralık dışı, başka şirket.
        fatura("H2-VD-3", SIRKET, c, BAS.plusDays(5), "900", "0", "ODENDI", Fatura.FaturaDurum.KESILDI);
        fatura("H2-VD-4", SIRKET, c, BAS.plusDays(6), "900", "0", "IPTAL", Fatura.FaturaDurum.KESILDI);
        fatura("H2-VD-5", SIRKET, c, BAS.minusDays(3), "900", "0", "ODENMEDI", Fatura.FaturaDurum.KESILDI);
        fatura("H2-VD-6", SIRKET, c, BAS.plusDays(10), "900", "0", "ODENMEDI", Fatura.FaturaDurum.IPTAL);
        fatura("H2-VD-7", BASKA_SIRKET, cari("Yabancı Cari", BASKA_SIRKET), BAS.plusDays(2),
                "900", "0", "ODENMEDI", Fatura.FaturaDurum.KESILDI);

        List<VadeTakvimProjeksiyon> satirlar = faturaRepository.vadeTakvimi(
                SIRKET, Fatura.FaturaDurum.KESILDI, KAPALI, BAS, BIT, PageRequest.of(0, 200));

        assertEquals(2, satirlar.size());
        assertEquals("H2-VD-2", satirlar.get(0).getFaturaNumarasi());
        assertEquals(BAS.plusDays(4), satirlar.get(0).getVadeTarihi());
        assertEquals("Vade Cari", satirlar.get(0).getCariAd());
        assertEquals(0, new BigDecimal("500").compareTo(satirlar.get(0).getGenelToplam()));
        assertEquals("H2-VD-1", satirlar.get(1).getFaturaNumarasi());
        assertEquals(0, new BigDecimal("400").compareTo(satirlar.get(1).getOdenenTutar()));
    }

    @Test
    void vadeTakvimiEnFazlaIstenenKayitSayisiniDoner() {
        CariHesap c = cari("Limit Cari", SIRKET);
        for (int i = 0; i < 5; i++) {
            fatura("H2-VD-L" + i, SIRKET, c, BAS.plusDays(i), "100", "0", "ODENMEDI", Fatura.FaturaDurum.KESILDI);
        }

        List<VadeTakvimProjeksiyon> satirlar = faturaRepository.vadeTakvimi(
                SIRKET, Fatura.FaturaDurum.KESILDI, KAPALI, BAS, BIT, PageRequest.of(0, 3));

        assertEquals(3, satirlar.size());
        assertEquals("H2-VD-L0", satirlar.get(0).getFaturaNumarasi());
        assertEquals("H2-VD-L2", satirlar.get(2).getFaturaNumarasi());
    }

    private CariHesap cari(String ad, Long sirketId) {
        return cariHesapRepository.save(CariHesap.builder()
                .ad(ad).tur("Musteri").bakiye(BigDecimal.ZERO)
                .sirketId(sirketId).build());
    }

    private void fatura(String numara, Long sirketId, CariHesap cari, LocalDate vade,
                        String genelToplam, String odenen, String odemeDurumu, Fatura.FaturaDurum durum) {
        faturaRepository.save(Fatura.builder()
                .faturaNumarasi(numara)
                .tarih(vade.minusDays(10))
                .vadeTarihi(vade)
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
