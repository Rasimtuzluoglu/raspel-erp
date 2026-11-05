package com.raspel.erp.repository;

import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.FaturaKalem;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.FaturaKalemRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.KarAnalizOzetProjeksiyon;
import com.raspel.erp.repository.ticaret.KarAnalizSatirProjeksiyon;
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
 * Kâr analizi JPQL sorgularını H2 üzerinde doğrular: SATIS + KESILDI filtresi,
 * tarih aralığı, tenant izolasyonu, maliyet anlık görüntüsü / birim maliyet
 * yedeği, maliyetsiz kalem sayacı, ürün-cari kırılımları, kâr azalan sıralama
 * ve ilk 10 sınırı.
 */
@SpringBootTest
@Transactional
class DashboardKarAnaliziRepositoryH2Test {

    private static final Long SIRKET = 8771L;
    private static final Long BASKA_SIRKET = 8772L;
    private static final Long KIRILIM_SIRKET = 8773L;
    private static final Long KIRILIM_BASKA_SIRKET = 8774L;

    private static final LocalDate BAS = LocalDate.of(2026, 11, 1);
    private static final LocalDate BIT = LocalDate.of(2026, 11, 30);
    private static final LocalDate TARIH = LocalDate.of(2026, 11, 15);

    private static final Fatura.FaturaTur SATIS = Fatura.FaturaTur.SATIS;
    private static final Fatura.FaturaTur ALIS = Fatura.FaturaTur.ALIS;
    private static final Fatura.FaturaDurum KESILDI = Fatura.FaturaDurum.KESILDI;
    private static final Fatura.FaturaDurum IPTAL = Fatura.FaturaDurum.IPTAL;

    @Autowired private FaturaKalemRepository faturaKalemRepository;
    @Autowired private FaturaRepository faturaRepository;
    @Autowired private CariHesapRepository cariHesapRepository;
    @Autowired private StokRepository stokRepository;

    @Test
    void ozetCiroMaliyetVeMaliyetsizKalemSayisiniSuzer() {
        CariHesap cari = cari("Analiz Cari", SIRKET);
        Fatura fatura = fatura("H2-KA-1", SIRKET, cari, TARIH, SATIS, KESILDI);
        // Maliyet anlık görüntüsü önceliklidir: 250 (5 × 50 değil).
        kalem(fatura, "Kalem A", 1L, "1000", "5", "50", "250");
        // maliyetTutar yoksa birimMaliyet × adet: 10 × 40 = 400.
        kalem(fatura, "Kalem B", 2L, "500", "10", "40", null);
        // İkisi de yok: maliyet 0, maliyetsiz sayılır.
        kalem(fatura, "Kalem C", 3L, "300", "2", null, null);

        // Hariç tutulması gerekenler: iptal, alış, aralık dışı, başka şirket.
        Fatura iptal = fatura("H2-KA-2", SIRKET, cari, TARIH, SATIS, IPTAL);
        kalem(iptal, "İptal Kalem", 4L, "9999", "1", "1", "1");
        Fatura alis = fatura("H2-KA-3", SIRKET, cari, TARIH, ALIS, KESILDI);
        kalem(alis, "Alış Kalem", 5L, "8888", "1", "1", "1");
        Fatura eski = fatura("H2-KA-4", SIRKET, cari, BAS.minusDays(1), SATIS, KESILDI);
        kalem(eski, "Eski Kalem", 6L, "7777", "1", "1", "1");
        Fatura yabanci = fatura("H2-KA-5", BASKA_SIRKET, cari("Yabancı", BASKA_SIRKET), TARIH, SATIS, KESILDI);
        kalem(yabanci, "Yabancı Kalem", 7L, "6666", "1", "1", "1");

        KarAnalizOzetProjeksiyon ozet = faturaKalemRepository.karAnaliziOzet(SIRKET, SATIS, KESILDI, BAS, BIT);

        assertEquals(0, new BigDecimal("1800").compareTo(ozet.getCiro()));
        assertEquals(0, new BigDecimal("650").compareTo(ozet.getMaliyet()));
        assertEquals(1L, faturaKalemRepository.maliyetsizKalemSayisi(SIRKET, SATIS, KESILDI, BAS, BIT));
    }

    @Test
    void bosDonemdeOzetNullDoner() {
        KarAnalizOzetProjeksiyon ozet = faturaKalemRepository.karAnaliziOzet(
                SIRKET, SATIS, KESILDI, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31));

        assertTrue(ozet == null || (ozet.getCiro() == null && ozet.getMaliyet() == null));
        assertEquals(0L, faturaKalemRepository.maliyetsizKalemSayisi(
                SIRKET, SATIS, KESILDI, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)));
    }

    @Test
    void urunKirilimiKarAzalanDonerVeOnIleSinirlanir() {
        CariHesap cari = cari("Karlı Cari", KIRILIM_SIRKET);
        Fatura fatura = fatura("H2-KA-K1", KIRILIM_SIRKET, cari, TARIH, SATIS, KESILDI);
        // 12 ürün; kâr 1200'den 100'e düşer (ciro 1300..200, maliyet 100).
        for (int i = 0; i < 12; i++) {
            Stok stok = stok("H2-KA-STK-" + i, "Ürün " + i, KIRILIM_SIRKET);
            kalem(fatura, "Ürün kalemi " + i, stok.getId(),
                    String.valueOf(1300 - i * 100), "1", "100", null);
        }
        // Ciro 0 olan ürün grubu listeye girmemeli.
        Stok bedelsiz = stok("H2-KA-STK-SIFIR", "Sıfır Ciro", KIRILIM_SIRKET);
        kalem(fatura, "Bedelsiz kalem", bedelsiz.getId(), "0", "1", "10", null);
        // Başka şirketin yüksek kârlı ürünü listeye girmemeli.
        Fatura yabanci = fatura("H2-KA-K2", KIRILIM_BASKA_SIRKET,
                cari("Yabancı Cari", KIRILIM_BASKA_SIRKET), TARIH, SATIS, KESILDI);
        Stok yabanciStok = stok("H2-KA-STK-Y", "Yabancı Ürün", KIRILIM_BASKA_SIRKET);
        kalem(yabanci, "Yabancı kalem", yabanciStok.getId(), "100000", "1", "1", null);

        List<KarAnalizSatirProjeksiyon> satirlar = faturaKalemRepository.karAnaliziUrunler(
                KIRILIM_SIRKET, SATIS, KESILDI, BAS, BIT, PageRequest.of(0, 10));

        assertEquals(10, satirlar.size());
        assertEquals("Ürün 0", satirlar.get(0).getAd());
        assertEquals(0, new BigDecimal("1300").compareTo(satirlar.get(0).getCiro()));
        assertEquals(0, new BigDecimal("1200").compareTo(
                satirlar.get(0).getCiro().subtract(satirlar.get(0).getMaliyet())));
        assertEquals("Ürün 9", satirlar.get(9).getAd());
        for (int i = 1; i < satirlar.size(); i++) {
            BigDecimal oncekiKar = satirlar.get(i - 1).getCiro().subtract(satirlar.get(i - 1).getMaliyet());
            BigDecimal simdikiKar = satirlar.get(i).getCiro().subtract(satirlar.get(i).getMaliyet());
            assertTrue(oncekiKar.compareTo(simdikiKar) >= 0);
        }
        assertTrue(satirlar.stream().noneMatch(s -> "Sıfır Ciro".equals(s.getAd())));
        assertTrue(satirlar.stream().noneMatch(s -> "Yabancı Ürün".equals(s.getAd())));
    }

    @Test
    void cariKirilimiKarAzalanDonerVeCariYoksaAtlanir() {
        CariHesap karli = cari("Karlı Cari", KIRILIM_SIRKET);
        Fatura fatura = fatura("H2-KA-C1", KIRILIM_SIRKET, karli, TARIH, SATIS, KESILDI);
        kalem(fatura, "Cari kalem 1", 101L, "1000", "1", "100", null);
        kalem(fatura, "Cari kalem 2", 102L, "500", "1", "100", null);

        Fatura ikinci = fatura("H2-KA-C2", KIRILIM_SIRKET, karli, TARIH.plusDays(1), SATIS, KESILDI);
        kalem(ikinci, "Cari kalem 3", 103L, "250", "1", "50", null);

        CariHesap azKarli = cari("Az Kârlı Cari", KIRILIM_SIRKET);
        Fatura az = fatura("H2-KA-C3", KIRILIM_SIRKET, azKarli, TARIH, SATIS, KESILDI);
        kalem(az, "Az kârlı kalem", 104L, "300", "1", "250", null);

        // Cari hesabı olmayan fatura cari kırılımına girmemeli.
        Fatura cariYok = fatura("H2-KA-C4", KIRILIM_SIRKET, null, TARIH, SATIS, KESILDI);
        kalem(cariYok, "Carisiz kalem", 105L, "9999", "1", "1", null);

        List<KarAnalizSatirProjeksiyon> satirlar = faturaKalemRepository.karAnaliziCariler(
                KIRILIM_SIRKET, SATIS, KESILDI, BAS, BIT, PageRequest.of(0, 10));

        assertEquals(2, satirlar.size());
        assertEquals("Karlı Cari", satirlar.get(0).getAd());
        // Aynı carinin iki faturası tek satırda birleşir: 1000 + 500 + 250.
        assertEquals(0, new BigDecimal("1750").compareTo(satirlar.get(0).getCiro()));
        assertEquals(0, new BigDecimal("250").compareTo(satirlar.get(0).getMaliyet()));
        assertEquals("Az Kârlı Cari", satirlar.get(1).getAd());
        assertTrue(satirlar.stream().noneMatch(s -> s.getId() == null));
    }

    private CariHesap cari(String ad, Long sirketId) {
        return cariHesapRepository.save(CariHesap.builder()
                .ad(ad).tur("Musteri").bakiye(BigDecimal.ZERO)
                .sirketId(sirketId).build());
    }

    private Stok stok(String stokKodu, String ad, Long sirketId) {
        return stokRepository.save(Stok.builder()
                .sirketId(sirketId)
                .stokKodu(stokKodu)
                .ad(ad)
                .miktar(BigDecimal.ONE)
                .fiyat(BigDecimal.ONE)
                .build());
    }

    private Fatura fatura(String numara, Long sirketId, CariHesap cari, LocalDate tarih,
                          Fatura.FaturaTur tur, Fatura.FaturaDurum durum) {
        return faturaRepository.save(Fatura.builder()
                .faturaNumarasi(numara)
                .tarih(tarih)
                .tur(tur)
                .durum(durum)
                // ara_toplam / kdv / genel_toplam nullable=false.
                .araToplam(BigDecimal.ZERO)
                .kdv(BigDecimal.ZERO)
                .genelToplam(BigDecimal.ZERO)
                .cariHesap(cari)
                .sirketId(sirketId)
                .build());
    }

    private void kalem(Fatura fatura, String aciklama, Long stokId, String tutar, String adet,
                       String birimMaliyet, String maliyetTutar) {
        faturaKalemRepository.save(FaturaKalem.builder()
                .fatura(fatura)
                .aciklama(aciklama)
                .adet(new BigDecimal(adet))
                .birimFiyat(new BigDecimal(tutar))
                .kdvOrani(BigDecimal.ZERO)
                .iskontoOrani(BigDecimal.ZERO)
                .tutar(new BigDecimal(tutar))
                .stokId(stokId)
                .birimMaliyet(birimMaliyet != null ? new BigDecimal(birimMaliyet) : null)
                .maliyetTutar(maliyetTutar != null ? new BigDecimal(maliyetTutar) : null)
                .build());
    }
}
