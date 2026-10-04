package com.raspel.erp.repository;

import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.service.sistem.RaporService;
import com.raspel.erp.dto.sistem.RaporDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vade yaşlandırma raporunun gerçek sorgusunu ve kovalama mantığını uçtan uca
 * doğrular.
 *
 * <p>Bu testin varlık sebebi bir canlı hata: kova gruplaması native
 * {@code CASE ... GROUP BY} sorgusunda yapılıyordu; Hibernate {@code :bugun}
 * parametresini pozisyonel {@code ?} olarak bağlayınca PostgreSQL
 * {@code column must appear in the GROUP BY clause} hatası veriyordu ve tüm
 * rapor 500 dönüyordu. H2 birim testleri bu hatayı göremediği için canlı
 * PostgreSQL'e karşı doğrulama şart.
 *
 * <p>Kovalama artık Java tarafında; burada hem sorgunun derlendiğini hem de
 * kova atamalarının doğru olduğunu doğruluyoruz.
 */
@SpringBootTest
// Spring context testler arası paylaşılır; rollback olmazsa bir testin
// oluşturduğu cari/fatura diğerlerinin sonuçlarını kirletir (toplamlar ve
// "tek cari olmalı" gibi sayımlar yanlış çıkar).
@Transactional
class YaslandirmaEntegrasyonH2Test {

    private static final Long SIRKET = 991L;
    private static final Long BASKA_SIRKET = 992L;
    private static final LocalDate REFERANS = LocalDate.of(2026, 3, 15);

    @Autowired private FaturaRepository faturaRepository;
    @Autowired private CariHesapRepository cariHesapRepository;
    @Autowired private RaporService raporService;

    private CariHesap cari(String ad, Long sirketId) {
        return cariHesapRepository.save(CariHesap.builder()
                .ad(ad).tur("Musteri").bakiye(BigDecimal.ZERO)
                .sirketId(sirketId).build());
    }

    private Fatura fatura(CariHesap cari, LocalDate vade, BigDecimal kalan) {
        return faturaRepository.save(Fatura.builder()
                .faturaNumarasi("FTR-" + System.nanoTime())
                .tarih(vade == null ? REFERANS.minusDays(10) : vade.minusDays(30))
                .tur(Fatura.FaturaTur.SATIS)
                .durum(Fatura.FaturaDurum.KESILDI)
                .odemeDurumu("ODENMEDI")
                // ara_toplam / kdv / genel_toplam nullable=false; yaşlandırma
                // yalnızca kalanTutar ve vadeTarihi okuduğu için tutarlar sadece
                // şema kısıtını geçecek kadar doldurulur.
                .araToplam(kalan)
                .kdv(BigDecimal.ZERO)
                .genelToplam(kalan)
                .kalanTutar(kalan)
                .vadeTarihi(vade)
                .cariHesap(cari)
                .sirketId(SIRKET)
                .build());
    }

    @Test
    void kovalarSorguVeJavaTarafiDogruCalisir() {
        CariHesap c = cari("Test Müşteri", SIRKET);
        fatura(c, REFERANS.plusDays(20), new BigDecimal("1000"));   // vadesi gelmemis
        fatura(c, REFERANS.minusDays(15), new BigDecimal("500"));   // 0-30
        fatura(c, REFERANS.minusDays(70), new BigDecimal("300"));   // 61-90
        fatura(c, REFERANS.minusDays(200), new BigDecimal("200"));  // 90+

        var rapor = raporService.yaslandirmaRaporu(SIRKET, REFERANS);

        assertEquals(1, rapor.getSatirlar().size(), "Tek cari olmalı");
        var satir = rapor.getSatirlar().get(0);
        assertEquals("Test Müşteri", satir.getCariAd());

var kovalar = satir.getKovalar();
        // Kalan tutarlar ölçekli (19,2) gelir; karşılaştırma compareTo ile.
        assertEquals(0, kovalar.get("VADEDI_GELMEMIS").compareTo(new BigDecimal("1000")));
        assertEquals(0, kovalar.get("GUN_0_30").compareTo(new BigDecimal("500")));
        assertEquals(0, kovalar.get("GUN_61_90").compareTo(new BigDecimal("300")));
        assertEquals(0, kovalar.get("GUN_90_PLUS").compareTo(new BigDecimal("200")));
        assertEquals(0, kovalar.get("GUN_31_60").compareTo(BigDecimal.ZERO));

        // 200 günlük en gecikmiş fatura maksimumu belirler.
        assertEquals(200, satir.getEnFazlaGecikmeGun());
        // Ortalama yalnızca üç gecikmiş fatura üzerinden: (15 + 70 + 200) / 3
        assertEquals(95.0d, satir.getOrtalamaGecikmeGun(), 0.01d);
        assertEquals(0, satir.getToplam().compareTo(new BigDecimal("2000")));
        assertEquals(0, satir.getGecikmisTutar().compareTo(new BigDecimal("1000")));

        var ozet = rapor.getOzet();
        assertEquals(1, ozet.getCariSayisi());
        assertEquals(0, ozet.getToplam().compareTo(new BigDecimal("2000")));
        assertEquals(0, ozet.getGecikmisTutar().compareTo(new BigDecimal("1000")));
        assertEquals(RaporService.YASLANDIRMA_KOVALARI, ozet.getKovaSirasi());
    }

    /** Ödenmiş ve iptal edilmiş faturalar tahsil edilecek kapsamına girmemeli. */
    @Test
    void kapaliOdemeliFaturalarHaraplanir() {
        CariHesap c = cari("Kapalı Cari", SIRKET);
        fatura(c, REFERANS.minusDays(90), new BigDecimal("999"));
        var odendi = fatura(c, REFERANS.minusDays(90), new BigDecimal("999"));
        odendi.setOdemeDurumu("ODENDI");
        faturaRepository.save(odendi);
        var iptal = fatura(c, REFERANS.minusDays(90), new BigDecimal("999"));
        iptal.setDurum(Fatura.FaturaDurum.IPTAL);
        faturaRepository.save(iptal);

        var rapor = raporService.yaslandirmaRaporu(SIRKET, REFERANS);

        var satir = rapor.getSatirlar().stream()
                .filter(s -> "Kapalı Cari".equals(s.getCariAd()))
                .findFirst().orElseThrow();
        assertEquals(0, satir.getToplam().compareTo(new BigDecimal("999")),
                "Yalnızca ödenmemiş KESILDI fatura sayılmalı");
    }

    /** Tenant izolasyonu: başka şirketin faturası rapora girmemeli. */
    @Test
    void tenantIzolasyonu() {
        CariHesap c = cari("Yabancı Cari", SIRKET);
        fatura(c, REFERANS.minusDays(10), new BigDecimal("111"));
        CariHesap yabanci = cari("Baska Sirket", BASKA_SIRKET);
        var yabanciFatura = fatura(yabanci, REFERANS.minusDays(300), new BigDecimal("9999"));
        yabanciFatura.setSirketId(BASKA_SIRKET);
        faturaRepository.save(yabanciFatura);

        var rapor = raporService.yaslandirmaRaporu(SIRKET, REFERANS);

        assertTrue(rapor.getSatirlar().stream().noneMatch(s -> "Baska Sirket".equals(s.getCariAd())),
                "Başka şirketin carisi rapora sızmamalı");
        assertEquals(0, rapor.getOzet().getToplam().compareTo(new BigDecimal("111")));
    }

    /** Ödenmiş fatura kalanTutar > 0 şartını sağlamıyorsa listelenmemeli. */
    @Test
    void kalanTutarSifirOlanFaturaDahilEdilmez() {
        CariHesap c = cari("Sıfır Kalan", SIRKET);
        fatura(c, REFERANS.minusDays(10), BigDecimal.ZERO);

        var rapor = raporService.yaslandirmaRaporu(SIRKET, REFERANS);

        assertTrue(rapor.getSatirlar().stream().noneMatch(s -> "Sıfır Kalan".equals(s.getCariAd())),
                "Kalan tutarı sıfır olan fatura alacak oluşturmaz");
    }

    @Test
    void vadesiNullFaturaVadesiGelmemisKovasinaDuser() {
        CariHesap c = cari("Vadesiz Cari", SIRKET);
        fatura(c, null, new BigDecimal("750"));

        var satir = raporService.yaslandirmaRaporu(SIRKET, REFERANS).getSatirlar().stream()
                .filter(s -> "Vadesiz Cari".equals(s.getCariAd()))
                .findFirst().orElseThrow();

assertEquals(0, satir.getKovalar().get("VADEDI_GELMEMIS").compareTo(new BigDecimal("750")));
        assertEquals(0, satir.getGecikmisTutar().compareTo(BigDecimal.ZERO));
        assertEquals(0, satir.getEnFazlaGecikmeGun());
    }

    /** Satırlar en çok geciken cari en üstte sıralanmalı. */
    @Test
    void satirlarGecikmeyeGoreAzalanSiralanir() {
        CariHesap az = cari("Az Gecikmiş", SIRKET);
        CariHesap cok = cari("Çok Gecikmiş", SIRKET);
        fatura(az, REFERANS.minusDays(5), new BigDecimal("100"));
        fatura(cok, REFERANS.minusDays(150), new BigDecimal("100"));

        var satirlar = raporService.yaslandirmaRaporu(SIRKET, REFERANS).getSatirlar();
        List<String> adlar = satirlar.stream().map(RaporDTO.YaslandirmaDTO::getCariAd).toList();

        assertTrue(adlar.indexOf("Çok Gecikmiş") < adlar.indexOf("Az Gecikmiş"),
                "En çok geciken cari başta olmalı, sıralama: " + adlar);
    }
}