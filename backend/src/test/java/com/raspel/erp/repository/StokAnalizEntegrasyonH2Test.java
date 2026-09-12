package com.raspel.erp.repository;

import com.raspel.erp.dto.envanter.AlisOzetDTO;
import com.raspel.erp.dto.envanter.AylikFiyatDTO;
import com.raspel.erp.dto.envanter.IslemSatirDTO;
import com.raspel.erp.dto.envanter.KarlilikDTO;
import com.raspel.erp.dto.envanter.MusteriAnalizDTO;
import com.raspel.erp.dto.envanter.SatisOzetDTO;
import com.raspel.erp.dto.envanter.StokAnalizDTO;
import com.raspel.erp.dto.envanter.TedarikciAnalizDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.FaturaKalem;
import com.raspel.erp.entity.ticaret.Iade;
import com.raspel.erp.entity.ticaret.IadeKalem;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.IadeKalemRepository;
import com.raspel.erp.repository.ticaret.IadeRepository;
import com.raspel.erp.service.envanter.StokAnalizService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H2 üzerinde ürün analizi JPQL sorgularının ve servis hesaplamalarının
 * uçtan uca doğruluğunu test eder (schema.sql + Hibernate DDL profili).
 */
@SpringBootTest
class StokAnalizEntegrasyonH2Test {

    @Autowired private StokRepository stokRepository;
    @Autowired private CariHesapRepository cariHesapRepository;
    @Autowired private FaturaRepository faturaRepository;
    @Autowired private IadeRepository iadeRepository;
    @Autowired private IadeKalemRepository iadeKalemRepository;
    @Autowired private StokAnalizService stokAnalizService;

    @Test
    void analizSorgulariVeHesaplamalarCalisir() {
        Long sirket = 88L;
        CariHesap tedarikci = cariHesapRepository.save(ornekCari(sirket, "Tedarikçi H2", "90001"));
        CariHesap musteri = cariHesapRepository.save(ornekCari(sirket, "Müşteri H2", "90002"));
        Stok stok = stokRepository.save(Stok.builder()
                .stokKodu("H2-88").ad("H2 Ürün").birim("ADET")
                .fiyat(BigDecimal.valueOf(100)).miktar(BigDecimal.valueOf(50))
                .sirketId(sirket).build());

        Fatura alis = Fatura.builder()
                .faturaNumarasi("H2-A-88").tarih(LocalDate.of(2026, 1, 10))
                .tur(Fatura.FaturaTur.ALIS).durum(Fatura.FaturaDurum.KESILDI)
                .cariHesap(tedarikci)
                .araToplam(BigDecimal.valueOf(1000)).kdv(BigDecimal.valueOf(200))
                .genelToplam(BigDecimal.valueOf(1200)).sirketId(sirket).build();
        alis.getKalemler().add(FaturaKalem.builder().fatura(alis)
                .aciklama("Alış kalemi").adet(10)
                .birimFiyat(BigDecimal.valueOf(100))
                .kdvOrani(BigDecimal.valueOf(20)).iskontoOrani(BigDecimal.TEN)
                .tutar(BigDecimal.valueOf(990)).stokId(stok.getId())
                .build());
        faturaRepository.save(alis);

        Fatura satis = Fatura.builder()
                .faturaNumarasi("H2-S-88").tarih(LocalDate.of(2026, 3, 5))
                .tur(Fatura.FaturaTur.SATIS).durum(Fatura.FaturaDurum.KESILDI)
                .cariHesap(musteri)
                .araToplam(BigDecimal.valueOf(1500)).kdv(BigDecimal.valueOf(300))
                .genelToplam(BigDecimal.valueOf(1800)).sirketId(sirket).build();
        satis.getKalemler().add(FaturaKalem.builder().fatura(satis)
                .aciklama("Satış kalemi").adet(5)
                .birimFiyat(BigDecimal.valueOf(300))
                .kdvOrani(BigDecimal.valueOf(20)).iskontoOrani(BigDecimal.ZERO)
                .tutar(BigDecimal.valueOf(1800)).stokId(stok.getId())
                .build());
        faturaRepository.save(satis);

        Iade iade = iadeRepository.save(Iade.builder()
                .faturaId(alis.getId()).tur("ALIS").tarih(LocalDate.of(2026, 1, 25))
                .tutar(BigDecimal.valueOf(240)).durum("TAMAMLANDI").sirketId(sirket).build());
        iadeKalemRepository.save(IadeKalem.builder()
                .iadeId(iade.getId()).stokId(stok.getId())
                .miktar(new BigDecimal("2")).birimFiyat(BigDecimal.valueOf(100))
                .kdvOrani(BigDecimal.valueOf(20)).tutar(BigDecimal.valueOf(240)).build());

        LocalDate bas = LocalDate.of(2026, 1, 1);
        LocalDate bit = LocalDate.of(2026, 12, 31);

        StokAnalizDTO analiz = stokAnalizService.analiz(sirket, stok.getId(), bas, bit);
        assertEquals("H2 Ürün", analiz.getStokAd());

        AlisOzetDTO alisOzet = analiz.getAlisOzet();
        // 10 - 2 (iade) = 8; net 90 birim fiyat (100 - %10 iskonto) -> 90*10=900, iade 2*100=200 -> 700/8=87.50
        assertEquals(0, new BigDecimal("8").compareTo(alisOzet.getToplamAlisMiktar()));
        assertEquals(0, new BigDecimal("700").compareTo(alisOzet.getToplamAlisTutari()));
        assertEquals(0, new BigDecimal("87.50").compareTo(alisOzet.getOrtalamaBirimFiyat()));

        SatisOzetDTO satisOzet = analiz.getSatisOzet();
        assertEquals(0, new BigDecimal("5").compareTo(satisOzet.getToplamSatisMiktar()));
        assertEquals(0, new BigDecimal("1500").compareTo(satisOzet.getToplamSatisTutari()));

        KarlilikDTO karlilik = analiz.getKarlilik();
        // maliyet 100; satış 300 -> birim kâr 200, toplam 200*5=1000, marj 1000/1500*100 = 66.67
        assertEquals(0, new BigDecimal("300").compareTo(karlilik.getOrtalamaSatisFiyati()));
        assertEquals(0, new BigDecimal("1000").compareTo(karlilik.getToplamBrutKar()));
        assertEquals(0, new BigDecimal("66.67").compareTo(karlilik.getBrutKarMarji()));

        List<TedarikciAnalizDTO> tedarikciler = stokAnalizService.tedarikciAnaliz(sirket, stok.getId(), bas, bit);
        assertEquals(1, tedarikciler.size());
        assertEquals("Tedarikçi H2", tedarikciler.get(0).getCariHesapAd());
        assertEquals(0, new BigDecimal("8").compareTo(tedarikciler.get(0).getToplamMiktar()));

        List<MusteriAnalizDTO> musteriler = stokAnalizService.musteriAnaliz(sirket, stok.getId(), bas, bit);
        assertEquals(1, musteriler.size());
        assertEquals("Müşteri H2", musteriler.get(0).getCariHesapAd());

        List<IslemSatirDTO> gecmis = stokAnalizService.islemGecmisi(sirket, stok.getId(), bas, bit, false);
        assertEquals(3, gecmis.size());
        assertEquals("SATIS", gecmis.get(0).getTur());
        assertEquals("ALIS_IADE", gecmis.get(1).getTur());
        assertEquals("ALIS", gecmis.get(2).getTur());

        List<AylikFiyatDTO> aylik = stokAnalizService.aylikFiyatGecmisi(sirket, stok.getId(), bas, bit);
        assertEquals(2, aylik.size());
        assertEquals(1, aylik.get(0).getAy());
        assertEquals(0, new BigDecimal("87.50").compareTo(aylik.get(0).getOrtalamaAlisFiyati()));
        assertEquals(0, BigDecimal.ZERO.compareTo(aylik.get(0).getOrtalamaSatisFiyati()));
        assertEquals(3, aylik.get(1).getAy());
        assertEquals(0, BigDecimal.ZERO.compareTo(aylik.get(1).getOrtalamaAlisFiyati()));
        assertEquals(0, new BigDecimal("300").compareTo(aylik.get(1).getOrtalamaSatisFiyati()));
    }

    private CariHesap ornekCari(Long sirketId, String ad, String vergi) {
        return CariHesap.builder()
                .ad(ad)
                .vergiNumarasi(vergi)
                .bakiye(BigDecimal.ZERO)
                .sirketId(sirketId)
                .olusturmaTarihi(LocalDateTime.now())
                .guncellemeTarihi(LocalDateTime.now())
                .build();
    }
}