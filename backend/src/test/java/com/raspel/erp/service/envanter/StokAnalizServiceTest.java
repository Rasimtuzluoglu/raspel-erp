package com.raspel.erp.service.envanter;

import com.raspel.erp.dto.envanter.AlisOzetDTO;
import com.raspel.erp.dto.envanter.AylikFiyatDTO;
import com.raspel.erp.dto.envanter.IslemGecmisiSayfaliDTO;
import com.raspel.erp.dto.envanter.IslemSatirDTO;
import com.raspel.erp.dto.envanter.KarlilikDTO;
import com.raspel.erp.dto.envanter.MusteriAnalizDTO;
import com.raspel.erp.dto.envanter.SatisOzetDTO;
import com.raspel.erp.dto.envanter.StokAnalizDTO;
import com.raspel.erp.dto.envanter.TedarikciAnalizDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.ticaret.FaturaAnalizSatirProjeksiyon;
import com.raspel.erp.repository.ticaret.FaturaKalemRepository;
import com.raspel.erp.repository.ticaret.IadeAnalizSatirProjeksiyon;
import com.raspel.erp.repository.ticaret.IadeKalemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StokAnalizServiceTest {

    @Mock
    private StokRepository stokRepository;
    @Mock
    private FaturaKalemRepository faturaKalemRepository;
    @Mock
    private IadeKalemRepository iadeKalemRepository;

    @InjectMocks
    private StokAnalizService service;

    private static final Long SIRKET = 1L;
    private static final Long STOK_ID = 10L;

    private Stok stok() {
        return Stok.builder()
                .id(STOK_ID).ad("Ürün A").stokKodu("STK-1").birim("ADET")
                .fiyat(new BigDecimal("100")).miktar(new BigDecimal("50"))
                .satisFiyati(new BigDecimal("180")).sirketId(SIRKET).build();
    }

    /** KDV dahil tutar, iskonto düşülmüş net fiyat ve adet/iskonto içeren fatura satırı. */
    private FaturaAnalizSatirProjeksiyon faturaSatir(LocalDate tarih, String no, Long cariId, String cariAd,
                                                      int adet, String birimFiyat, String iskonto) {
        return new FaturaAnalizSatirProjeksiyon() {
            @Override public LocalDate getFaturaTarihi() { return tarih; }
            @Override public String getFaturaNumarasi() { return no; }
            @Override public Long getCariHesapId() { return cariId; }
            @Override public String getCariHesapAd() { return cariAd; }
            @Override public Integer getAdet() { return adet; }
            @Override public BigDecimal getBirimFiyat() { return new BigDecimal(birimFiyat); }
            @Override public BigDecimal getIskontoOrani() { return new BigDecimal(iskonto); }
        };
    }

    private IadeAnalizSatirProjeksiyon iadeSatir(Long iadeId, LocalDate tarih, String tur, Long cariId,
                                                 String cariAd, String miktar, String birimFiyat) {
        return new IadeAnalizSatirProjeksiyon() {
            @Override public Long getIadeId() { return iadeId; }
            @Override public LocalDate getIadeTarihi() { return tarih; }
            @Override public String getIadeTuru() { return tur; }
            @Override public Long getCariHesapId() { return cariId; }
            @Override public String getCariHesapAd() { return cariAd; }
            @Override public BigDecimal getMiktar() { return new BigDecimal(miktar); }
            @Override public BigDecimal getBirimFiyat() { return new BigDecimal(birimFiyat); }
        };
    }

    private void stokVar() {
        when(stokRepository.findById(STOK_ID)).thenReturn(Optional.of(stok()));
    }

    private void alisSatirlari(List<FaturaAnalizSatirProjeksiyon> faturalar, List<IadeAnalizSatirProjeksiyon> iadeler) {
        when(faturaKalemRepository.analizSatirlari(eq(STOK_ID), eq(SIRKET), eq(Fatura.FaturaTur.ALIS),
                eq(Fatura.FaturaDurum.KESILDI), any(), any())).thenReturn(faturalar);
        when(iadeKalemRepository.analizSatirlari(eq(STOK_ID), eq(SIRKET), eq("ALIS"), eq("TAMAMLANDI"), any(), any()))
                .thenReturn(iadeler);
    }

    private void satisSatirlari(List<FaturaAnalizSatirProjeksiyon> faturalar, List<IadeAnalizSatirProjeksiyon> iadeler) {
        when(faturaKalemRepository.analizSatirlari(eq(STOK_ID), eq(SIRKET), eq(Fatura.FaturaTur.SATIS),
                eq(Fatura.FaturaDurum.KESILDI), any(), any())).thenReturn(faturalar);
        when(iadeKalemRepository.analizSatirlari(eq(STOK_ID), eq(SIRKET), eq("SATIS"), eq("TAMAMLANDI"), any(), any()))
                .thenReturn(iadeler);
    }

    // ---------- Erişim / yetki ----------

    @Test
    void analiz_stokBulunamazsaHataFirlatir() {
        when(stokRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.analiz(SIRKET, 99L, LocalDate.now(), LocalDate.now()));
    }

    @Test
    void analiz_baskaSirketStoguIcinYetkiHatasi() {
        Stok baskaSirket = Stok.builder().id(STOK_ID).ad("Başka").sirketId(2L).build();
        when(stokRepository.findById(STOK_ID)).thenReturn(Optional.of(baskaSirket));
        assertThrows(BusinessException.class, () -> service.analiz(SIRKET, STOK_ID, LocalDate.now(), LocalDate.now()));
    }

    // ---------- Alış özeti ----------

    @Test
    void alisOzet_tartiliOrtalamaHesaplar() {
        stokVar();
        alisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 1, 5), "F001", 1L, "Tedarikçi A", 10, "100", "0"),
                faturaSatir(LocalDate.of(2026, 2, 5), "F002", 1L, "Tedarikçi A", 20, "150", "0")),
                List.of());
        AlisOzetDTO ozet = service.alisOzet(SIRKET, STOK_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(0, new BigDecimal("30").compareTo(ozet.getToplamAlisMiktar()));
        assertEquals(0, new BigDecimal("4000").compareTo(ozet.getToplamAlisTutari()));
        assertEquals(0, new BigDecimal("133.33").compareTo(ozet.getOrtalamaBirimFiyat()));
        assertEquals(0, new BigDecimal("150").compareTo(ozet.getSonAlisFiyati()));
        assertEquals(0, new BigDecimal("100").compareTo(ozet.getEnDusukAlisFiyati()));
        assertEquals(0, new BigDecimal("150").compareTo(ozet.getEnYuksekAlisFiyati()));
        assertEquals(LocalDate.of(2026, 2, 5), ozet.getSonAlisTarihi());
        assertEquals(2, ozet.getIslemSayisi());
    }

    @Test
    void alisOzet_iskontoNetFiyataYansir() {
        stokVar();
        // 100 birim fiyat, %20 iskonto -> net 80
        alisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 1, 5), "F001", 1L, "Tedarikçi A", 10, "100", "20")),
                List.of());
        AlisOzetDTO ozet = service.alisOzet(SIRKET, STOK_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(0, new BigDecimal("80").compareTo(ozet.getOrtalamaBirimFiyat()));
        assertEquals(0, new BigDecimal("800").compareTo(ozet.getToplamAlisTutari()));
    }

    @Test
    void alisOzet_alisIadesiMiktarVeTutariDuser() {
        stokVar();
        alisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 1, 5), "F001", 1L, "Tedarikçi A", 10, "100", "0")),
                List.of(iadeSatir(77L, LocalDate.of(2026, 1, 20), "ALIS", 1L, "Tedarikçi A", "4", "100")));
        AlisOzetDTO ozet = service.alisOzet(SIRKET, STOK_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(0, new BigDecimal("6").compareTo(ozet.getToplamAlisMiktar()));
        assertEquals(0, new BigDecimal("600").compareTo(ozet.getToplamAlisTutari()));
    }

    @Test
    void alisOzet_hareketYoksaSifirDonderir() {
        stokVar();
        alisSatirlari(List.of(), List.of());
        AlisOzetDTO ozet = service.alisOzet(SIRKET, STOK_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(0, BigDecimal.ZERO.compareTo(ozet.getToplamAlisMiktar()));
        assertEquals(0, BigDecimal.ZERO.compareTo(ozet.getOrtalamaBirimFiyat()));
        assertEquals(0, ozet.getIslemSayisi());
    }

    // ---------- Satış özeti ----------

    @Test
    void satisOzet_tartiliOrtalamaHesaplar() {
        stokVar();
        satisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 3, 1), "S001", 2L, "Müşteri X", 5, "200", "0"),
                faturaSatir(LocalDate.of(2026, 4, 1), "S002", 2L, "Müşteri X", 5, "300", "0")),
                List.of());
        SatisOzetDTO ozet = service.satisOzet(SIRKET, STOK_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(0, new BigDecimal("10").compareTo(ozet.getToplamSatisMiktar()));
        assertEquals(0, new BigDecimal("2500").compareTo(ozet.getToplamSatisTutari()));
        assertEquals(0, new BigDecimal("250").compareTo(ozet.getOrtalamaBirimFiyat()));
    }

    @Test
    void satisOzet_satisIadesiDusulur() {
        stokVar();
        satisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 3, 1), "S001", 2L, "Müşteri X", 5, "200", "0")),
                List.of(iadeSatir(5L, LocalDate.of(2026, 3, 10), "SATIS", 2L, "Müşteri X", "2", "200")));
        SatisOzetDTO ozet = service.satisOzet(SIRKET, STOK_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(0, new BigDecimal("3").compareTo(ozet.getToplamSatisMiktar()));
        assertEquals(0, new BigDecimal("600").compareTo(ozet.getToplamSatisTutari()));
    }

    // ---------- Kârlılık ----------

    @Test
    void karlilik_brutKarVeMarjHesaplar() {
        stokVar();
        satisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 3, 1), "S001", 2L, "Müşteri X", 2, "150", "0")),
                List.of());
        KarlilikDTO k = service.karlilik(SIRKET, STOK_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(0, new BigDecimal("100").compareTo(k.getOrtalamaMaliyet()));
        assertEquals(0, new BigDecimal("150").compareTo(k.getOrtalamaSatisFiyati()));
        assertEquals(0, new BigDecimal("50").compareTo(k.getBirimBrutKar()));
        assertEquals(0, new BigDecimal("100").compareTo(k.getToplamBrutKar()));
        assertEquals(0, new BigDecimal("33.33").compareTo(k.getBrutKarMarji()));
        assertEquals(0, new BigDecimal("5000").compareTo(k.getStokMaliyeti()));
    }

    @Test
    void karlilik_satisIadesiKariDusurur() {
        stokVar();
        satisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 3, 1), "S001", 2L, "Müşteri X", 2, "150", "0")),
                List.of(iadeSatir(5L, LocalDate.of(2026, 3, 10), "SATIS", 2L, "Müşteri X", "1", "150")));
        KarlilikDTO k = service.karlilik(SIRKET, STOK_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(1, k.getSatilanMiktar());
        assertEquals(0, new BigDecimal("50").compareTo(k.getToplamBrutKar()));
        assertEquals(0, new BigDecimal("150").compareTo(k.getSatisIadeTutari()));
    }

    @Test
    void karlilik_satisYoksaSifirBolenYoktur() {
        stokVar();
        satisSatirlari(List.of(), List.of());
        KarlilikDTO k = service.karlilik(SIRKET, STOK_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(0, BigDecimal.ZERO.compareTo(k.getToplamBrutKar()));
        assertEquals(0, BigDecimal.ZERO.compareTo(k.getBrutKarMarji()));
        assertEquals(0, new BigDecimal("100").compareTo(k.getOrtalamaMaliyet()));
    }

    // ---------- Tedarikçi / Müşteri ----------

    @Test
    void tedarikciAnaliz_gruplarveOzetler() {
        stokVar();
        alisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 1, 5), "F001", 1L, "Tedarikçi A", 10, "100", "0"),
                faturaSatir(LocalDate.of(2026, 2, 5), "F002", 3L, "Tedarikçi B", 5, "200", "0")),
                List.of());
        List<TedarikciAnalizDTO> list = service.tedarikciAnaliz(SIRKET, STOK_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(2, list.size());
        TedarikciAnalizDTO once = list.get(0);
        assertEquals("Tedarikçi A", once.getCariHesapAd());
        assertEquals(0, new BigDecimal("10").compareTo(once.getToplamMiktar()));
        assertEquals(0, new BigDecimal("1000").compareTo(once.getToplamTutar()));
        assertEquals(LocalDate.of(2026, 2, 5), list.get(1).getSonAlisTarihi());
    }

    @Test
    void musteriAnaliz_carisizSatirlarAtlanir() {
        stokVar();
        satisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 3, 1), "S001", null, null, 5, "200", "0"),
                faturaSatir(LocalDate.of(2026, 4, 1), "S002", 2L, "Müşteri X", 3, "250", "0")),
                List.of());
        List<MusteriAnalizDTO> list = service.musteriAnaliz(SIRKET, STOK_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(1, list.size());
        assertEquals("Müşteri X", list.get(0).getCariHesapAd());
        assertEquals(0, new BigDecimal("3").compareTo(list.get(0).getToplamMiktar()));
    }

    // ---------- İşlem geçmişi ----------

    @Test
    void islemGecmisi_tariheGoreAzalanSiralar() {
        stokVar();
        when(faturaKalemRepository.analizSatirlari(eq(STOK_ID), eq(SIRKET), eq(Fatura.FaturaTur.ALIS),
                eq(Fatura.FaturaDurum.KESILDI), any(), any()))
                .thenReturn(List.of(faturaSatir(LocalDate.of(2026, 1, 5), "F001", 1L, "Tedarikçi A", 10, "100", "0")));
        when(faturaKalemRepository.analizSatirlari(eq(STOK_ID), eq(SIRKET), eq(Fatura.FaturaTur.SATIS),
                eq(Fatura.FaturaDurum.KESILDI), any(), any()))
                .thenReturn(List.of(faturaSatir(LocalDate.of(2026, 3, 1), "S001", 2L, "Müşteri X", 5, "200", "0")));
        when(iadeKalemRepository.analizSatirlari(eq(STOK_ID), eq(SIRKET), eq("ALIS"), eq("TAMAMLANDI"), any(), any()))
                .thenReturn(List.of());
        when(iadeKalemRepository.analizSatirlari(eq(STOK_ID), eq(SIRKET), eq("SATIS"), eq("TAMAMLANDI"), any(), any()))
                .thenReturn(List.of(iadeSatir(5L, LocalDate.of(2026, 4, 2), "SATIS", 2L, "Müşteri X", "1", "200")));
        List<IslemSatirDTO> satirlar = service.islemGecmisi(SIRKET, STOK_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false);
        assertEquals(3, satirlar.size());
        assertEquals("SATIS_IADE", satirlar.get(0).getTur());
        assertEquals("SATIS", satirlar.get(1).getTur());
        assertEquals("ALIS", satirlar.get(2).getTur());
        assertEquals("İade #5", satirlar.get(0).getBelgeNo());
    }

    @Test
    void islemGecmisi_limitIleSinirlar() {
        stokVar();
        when(faturaKalemRepository.analizSatirlari(eq(STOK_ID), eq(SIRKET), eq(Fatura.FaturaTur.ALIS),
                eq(Fatura.FaturaDurum.KESILDI), any(), any()))
                .thenReturn(List.of(
                        faturaSatir(LocalDate.of(2026, 1, 5), "F001", 1L, "Tedarikçi A", 10, "100", "0"),
                        faturaSatir(LocalDate.of(2026, 1, 6), "F002", 1L, "Tedarikçi A", 10, "100", "0"),
                        faturaSatir(LocalDate.of(2026, 1, 7), "F003", 1L, "Tedarikçi A", 10, "100", "0")));
        when(faturaKalemRepository.analizSatirlari(eq(STOK_ID), eq(SIRKET), eq(Fatura.FaturaTur.SATIS),
                eq(Fatura.FaturaDurum.KESILDI), any(), any())).thenReturn(List.of());
        when(iadeKalemRepository.analizSatirlari(eq(STOK_ID), eq(SIRKET), eq("ALIS"), eq("TAMAMLANDI"), any(), any()))
                .thenReturn(List.of());
        when(iadeKalemRepository.analizSatirlari(eq(STOK_ID), eq(SIRKET), eq("SATIS"), eq("TAMAMLANDI"), any(), any()))
                .thenReturn(List.of());
        List<IslemSatirDTO> sinirli = service.islemGecmisi(SIRKET, STOK_ID,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false, 2);
        assertEquals(2, sinirli.size());
        assertEquals("F003", sinirli.get(0).getBelgeNo());
        assertEquals("F002", sinirli.get(1).getBelgeNo());
        List<IslemSatirDTO> tamListe = service.islemGecmisi(SIRKET, STOK_ID,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false);
        assertEquals(3, tamListe.size());
    }

    @Test
    void analiz_baslangicBitisTersIseHataFirlatir() {
        assertThrows(BusinessException.class, () -> service.analiz(SIRKET, STOK_ID,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1)));
    }

    @Test
    void islemGecmisiSayfali_toplamVeSayfalamaYapar() {
        stokVar();
        alisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 1, 5), "F001", 1L, "Tedarikçi A", 10, "100", "0"),
                faturaSatir(LocalDate.of(2026, 1, 6), "F002", 1L, "Tedarikçi A", 10, "100", "0"),
                faturaSatir(LocalDate.of(2026, 1, 7), "F003", 1L, "Tedarikçi A", 10, "100", "0")),
                List.of());
        satisSatirlari(List.of(), List.of());
        IslemGecmisiSayfaliDTO sayfa0 = service.islemGecmisiSayfali(SIRKET, STOK_ID,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false, 0, 2);
        assertEquals(3, sayfa0.getToplam());
        assertEquals(2, sayfa0.getToplamSayfa());
        assertEquals("F003", sayfa0.getSatirlar().get(0).getBelgeNo());
        assertEquals("F002", sayfa0.getSatirlar().get(1).getBelgeNo());
        IslemGecmisiSayfaliDTO sayfa1 = service.islemGecmisiSayfali(SIRKET, STOK_ID,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false, 1, 2);
        assertEquals(1, sayfa1.getSatirlar().size());
        assertEquals("F001", sayfa1.getSatirlar().get(0).getBelgeNo());
        IslemGecmisiSayfaliDTO tasanSayfa = service.islemGecmisiSayfali(SIRKET, STOK_ID,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false, 5, 2);
        assertTrue(tasanSayfa.getSatirlar().isEmpty());
        assertEquals(5, tasanSayfa.getSayfa());
        assertEquals(3, tasanSayfa.getToplam());
    }

    @Test
    void islemGecmisiSayfali_boyutSinirlanir() {
        stokVar();
        alisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 1, 5), "F001", 1L, "Tedarikçi A", 10, "100", "0")),
                List.of());
        satisSatirlari(List.of(), List.of());
        IslemGecmisiSayfaliDTO sonuc = service.islemGecmisiSayfali(SIRKET, STOK_ID,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false, 0, 5000);
        assertEquals(200, sonuc.getBoyut());
    }

    @Test
    void tedarikciAnaliz_limitleSinirlar() {
        stokVar();
        List<FaturaAnalizSatirProjeksiyon> faturalar = new ArrayList<>();
        for (long i = 1; i <= 501; i++) {
            faturalar.add(faturaSatir(LocalDate.of(2026, 1, 5), "F" + i, i, "Tedarikçi " + i, 10, "100", "0"));
        }
        alisSatirlari(faturalar, List.of());
        List<TedarikciAnalizDTO> sonuc = service.tedarikciAnaliz(SIRKET, STOK_ID,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(500, sonuc.size());
    }

    @Test
    void aylikFiyat_aySayisiniSinirlar() {
        stokVar();
        List<FaturaAnalizSatirProjeksiyon> faturalar = new ArrayList<>();
        for (int i = 0; i < 601; i++) {
            faturalar.add(faturaSatir(LocalDate.of(2026 - i / 12, (i % 12) + 1, 5), "F" + i, 1L, "Tedarikçi A", 10, "100", "0"));
        }
        alisSatirlari(faturalar, List.of());
        satisSatirlari(List.of(), List.of());
        List<AylikFiyatDTO> aylar = service.aylikFiyatGecmisi(SIRKET, STOK_ID,
                LocalDate.of(2000, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(600, aylar.size());
    }

    // ---------- Aylık fiyat geçmişi ----------

    @Test
    void aylikFiyat_ayBazindaGruplar() {
        stokVar();
        alisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 1, 5), "F001", 1L, "Tedarikçi A", 10, "100", "0"),
                faturaSatir(LocalDate.of(2026, 1, 20), "F002", 1L, "Tedarikçi A", 10, "200", "0")),
                List.of());
        satisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 1, 25), "S001", 2L, "Müşteri X", 10, "250", "0")),
                List.of());
        List<AylikFiyatDTO> aylar = service.aylikFiyatGecmisi(SIRKET, STOK_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(1, aylar.size());
        AylikFiyatDTO ay = aylar.get(0);
        assertEquals(2026, ay.getYil());
        assertEquals(1, ay.getAy());
        assertEquals(0, new BigDecimal("150").compareTo(ay.getOrtalamaAlisFiyati()));
        assertEquals(0, new BigDecimal("250").compareTo(ay.getOrtalamaSatisFiyati()));
        assertEquals(0, new BigDecimal("20").compareTo(ay.getToplamAlisMiktar()));
        assertEquals(0, new BigDecimal("10").compareTo(ay.getToplamSatisMiktar()));
    }

    @Test
    void kombineAnaliz_ucBolumuDoldurur() {
        stokVar();
        alisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 1, 5), "F001", 1L, "Tedarikçi A", 10, "100", "0")),
                List.of());
        satisSatirlari(List.of(
                faturaSatir(LocalDate.of(2026, 3, 1), "S001", 2L, "Müşteri X", 2, "150", "0")),
                List.of());
        StokAnalizDTO analiz = service.analiz(SIRKET, STOK_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals("Ürün A", analiz.getStokAd());
        assertEquals(0, new BigDecimal("10").compareTo(analiz.getAlisOzet().getToplamAlisMiktar()));
        assertEquals(0, new BigDecimal("2").compareTo(analiz.getSatisOzet().getToplamSatisMiktar()));
        assertEquals(0, new BigDecimal("100").compareTo(analiz.getKarlilik().getToplamBrutKar()));
    }
}