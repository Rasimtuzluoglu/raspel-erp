package com.raspel.erp.service;

import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.dto.sistem.RaporDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.FaturaKalem;
import com.raspel.erp.entity.finans.Hareket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.repository.ticaret.FaturaKalemRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.service.finans.HareketService;
import com.raspel.erp.service.sistem.RaporService;

@ExtendWith(MockitoExtension.class)
class RaporServiceTest {

    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private HareketRepository hareketRepository;
    @Mock private FaturaRepository faturaRepository;
    @Mock private com.raspel.erp.repository.ticaret.FaturaKalemRepository faturaKalemRepository;
    @Mock private com.raspel.erp.repository.envanter.StokRepository stokRepository;
    @Mock private com.raspel.erp.repository.finans.KasaRepository kasaRepository;
    @Mock private com.raspel.erp.repository.finans.BankaRepository bankaRepository;
    @Mock private CariHesapService cariHesapService;
    @Mock private HareketService hareketService;
    @InjectMocks private RaporService raporService;

    private CariHesap createCariHesap() {
        CariHesap c = new CariHesap();
        c.setId(1L);
        c.setAd("Test Cari");
        c.setBakiye(BigDecimal.valueOf(5000));
        c.setOlusturmaTarihi(LocalDateTime.now());
        c.setGuncellemeTarihi(LocalDateTime.now());
        return c;
    }

    private Hareket createHareket() {
        CariHesap cari = createCariHesap();
        Hareket h = new Hareket();
        h.setId(1L);
        h.setCariHesap(cari);
        h.setTur(Hareket.HareketTuru.TAHSILAT);
        h.setTutar(BigDecimal.valueOf(1000));
        h.setHareketTarihi(LocalDate.now());
        h.setOlusturmaTarihi(LocalDateTime.now());
        return h;
    }

    @Test
    void cariEkstreGetir_returnsEkstre() {
        CariHesap cari = createCariHesap();
        when(cariHesapRepository.findById(1L)).thenReturn(java.util.Optional.of(cari));
        Hareket hareket = createHareket();
        when(hareketRepository.findByCariHesapIdAndHareketTarihiBetweenOrderByHareketTarihiAsc(any(), any(), any()))
                .thenReturn(List.of(hareket));
        HareketDTO dto = HareketDTO.builder().tur("TAHSILAT").tutar(BigDecimal.valueOf(1000)).build();
        when(hareketService.entityDTOyeCevir(any(Hareket.class))).thenReturn(dto);
        var result = raporService.cariEkstreGetir(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals("Test Cari", result.getCariAd());
    }

    @Test
    void cariEkstreGetir_throwsWhenCariNotFound() {
        when(cariHesapRepository.findById(99L)).thenReturn(java.util.Optional.empty());
        assertThrows(RuntimeException.class, () -> raporService.cariEkstreGetir(99L, LocalDate.now(), LocalDate.now()));
    }

    @Test
    void gelirGiderOzeti_returnsOzet() {
        Hareket tahsilat = createHareket();
        Hareket odeme = createHareket();
        odeme.setTur(Hareket.HareketTuru.ODEME);
        when(hareketRepository.findBySirketIdAndHareketTarihiBetweenOrderByHareketTarihiAsc(any(), any(), any()))
                .thenReturn(List.of(tahsilat, odeme));
        var result = raporService.gelirGiderOzeti(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 1L);
        assertEquals(BigDecimal.valueOf(1000), result.getToplamGelir());
    }

    @Test
    void kdvRaporu_returnsKdv() {
        Fatura fatura = new Fatura();
        fatura.setTur(Fatura.FaturaTur.SATIS);
        fatura.setDurum(Fatura.FaturaDurum.KESILDI);
        fatura.setKdv(BigDecimal.valueOf(200));
        fatura.setTarih(LocalDate.now());
        Fatura faturaAlis = new Fatura();
        faturaAlis.setTur(Fatura.FaturaTur.ALIS);
        faturaAlis.setDurum(Fatura.FaturaDurum.KESILDI);
        faturaAlis.setKdv(BigDecimal.valueOf(100));
        faturaAlis.setTarih(LocalDate.now());
        when(faturaRepository.findBySirketIdOrderByTarihDesc(1L)).thenReturn(List.of(fatura, faturaAlis));
        var result = raporService.kdvRaporu(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 1L);
        assertEquals(BigDecimal.valueOf(200), result.getToplamKdvCikis());
        assertEquals(BigDecimal.valueOf(100), result.getToplamKdvGiris());
        assertEquals(BigDecimal.valueOf(100), result.getKdvFarki());
    }

    @Test
    void yaslandirmaRaporu_returnsYaslandirma() {
        CariHesap cari1 = createCariHesap();
        cari1.setBakiye(BigDecimal.valueOf(3000));
        cari1.setGuncellemeTarihi(LocalDateTime.now().minusDays(45));
        CariHesap cari2 = createCariHesap();
        cari2.setId(2L);
        cari2.setAd("Cari 2");
        cari2.setBakiye(BigDecimal.valueOf(1000));
        cari2.setGuncellemeTarihi(LocalDateTime.now().minusDays(15));
        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of(cari1, cari2));
        var result = raporService.yaslandirmaRaporu(1L);
        assertEquals(2, result.size());
        assertEquals("31-60 Gün", result.get(0).getAralik());
        assertEquals("0-30 Gün", result.get(1).getAralik());
    }

    @Test
    void kdvBeyannameGetir_hesaplananVeIndirilecekKdv() {
        LocalDate ayIci = LocalDate.of(2026, 7, 15);
        Fatura satis = new Fatura();
        satis.setId(1L);
        satis.setTur(Fatura.FaturaTur.SATIS);
        satis.setDurum(Fatura.FaturaDurum.KESILDI);
        satis.setTarih(ayIci);
        Fatura alis = new Fatura();
        alis.setId(2L);
        alis.setTur(Fatura.FaturaTur.ALIS);
        alis.setDurum(Fatura.FaturaDurum.KESILDI);
        alis.setTarih(ayIci);

        when(faturaRepository.findBySirketIdOrderByTarihDesc(1L)).thenReturn(List.of(satis, alis));
        // 1.180 TL KDV dahil, %18 → matrah 1.000, KDV 180
        when(faturaKalemRepository.findByFaturaId(1L))
                .thenReturn(List.of(FaturaKalem.builder().kdvOrani(new BigDecimal("18")).tutar(BigDecimal.valueOf(1180)).build()));
        when(faturaKalemRepository.findByFaturaId(2L))
                .thenReturn(List.of(FaturaKalem.builder().kdvOrani(new BigDecimal("20")).tutar(BigDecimal.valueOf(1200)).build()));

        var result = raporService.kdvBeyannameGetir("2026-07", 1L);

        assertEquals(0, BigDecimal.valueOf(180).compareTo(result.getToplamHesaplananKdv()));
        assertEquals(0, BigDecimal.valueOf(200).compareTo(result.getToplamIndirilecekKdv()));
        assertEquals(0, BigDecimal.valueOf(20).compareTo(result.getDevredenKdv()));
    }

    @Test
    void baBsGetir_esiginUzerindekiKayitlariListeler() {
        LocalDate ayIci = LocalDate.of(2026, 7, 10);
        Fatura buyuk = new Fatura();
        buyuk.setId(1L);
        buyuk.setFaturaNumarasi("FTR-1");
        buyuk.setTur(Fatura.FaturaTur.SATIS);
        buyuk.setDurum(Fatura.FaturaDurum.KESILDI);
        buyuk.setTarih(ayIci);
        buyuk.setAraToplam(BigDecimal.valueOf(8000));
        buyuk.setKdv(BigDecimal.valueOf(1440));
        buyuk.setGenelToplam(BigDecimal.valueOf(9440));
        Fatura kucuk = new Fatura();
        kucuk.setId(2L);
        kucuk.setFaturaNumarasi("FTR-2");
        kucuk.setTur(Fatura.FaturaTur.SATIS);
        kucuk.setDurum(Fatura.FaturaDurum.KESILDI);
        kucuk.setTarih(ayIci);
        kucuk.setGenelToplam(BigDecimal.valueOf(1000));

        when(faturaRepository.findBySirketIdOrderByTarihDesc(1L)).thenReturn(List.of(buyuk, kucuk));

        var result = raporService.baBsGetir("2026-07", "BS", new BigDecimal("5000"), 1L);

        assertEquals("BS", result.getTur());
        assertEquals(1, result.getKayitlar().size());
        assertEquals("FTR-1", result.getKayitlar().get(0).getFaturaNo());
        assertEquals(BigDecimal.valueOf(9440), result.getToplamTutar());
    }

    @Test
    void tedarikciUrunRaporu_returnsGrouped() {
        when(faturaKalemRepository.tedarikciUrunler(eq(1L), eq(Fatura.FaturaTur.ALIS), eq(Fatura.FaturaDurum.KESILDI)))
                .thenReturn(List.of(new com.raspel.erp.repository.ticaret.TedarikciUrunProjeksiyon() {
                    public Long getCariHesapId() { return 1L; }
                    public String getCariHesapAd() { return "ABC Fabrika"; }
                    public Long getStokId() { return 10L; }
                    public Long getToplamMiktar() { return 50L; }
                    public BigDecimal getSonBirimFiyat() { return BigDecimal.valueOf(120); }
                    public LocalDate getSonTarih() { return LocalDate.of(2026, 7, 1); }
                }));

        com.raspel.erp.entity.envanter.Stok stok = new com.raspel.erp.entity.envanter.Stok();
        stok.setId(10L);
        stok.setAd("MDF 18mm");
        stok.setStokKodu("MDF-18");
        when(stokRepository.findAllById(java.util.List.of(10L))).thenReturn(java.util.List.of(stok));

        var result = raporService.tedarikciUrunRaporu(1L);

        assertEquals(1, result.size());
        assertEquals("ABC Fabrika", result.get(0).getCariHesapAd());
        assertEquals("MDF 18mm", result.get(0).getStokAd());
        assertEquals(50L, result.get(0).getToplamMiktar());
    }

    @Test
    void urunKarlilikRaporu_returnsMargin() {
        com.raspel.erp.entity.envanter.Stok stok = new com.raspel.erp.entity.envanter.Stok();
        stok.setId(1L);
        stok.setAd("MDF 18mm");
        stok.setStokKodu("MDF-18");
        stok.setFiyat(BigDecimal.valueOf(100));
        stok.setSatisFiyati(BigDecimal.valueOf(150));
        when(stokRepository.findBySirketIdOrderByAd(1L)).thenReturn(java.util.List.of(stok));

        var result = raporService.urunKarlilikRaporu(1L);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getKar().compareTo(BigDecimal.valueOf(50)));
        assertEquals(0, result.get(0).getKarMarji().compareTo(new BigDecimal("33.33")));
    }

    @Test
    void nakitAkisiProjeksiyonu_calculatesDailyAndCumulative() {
        com.raspel.erp.entity.finans.Kasa k = new com.raspel.erp.entity.finans.Kasa();
        k.setBakiye(BigDecimal.valueOf(10000));
        when(kasaRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of(k));

        com.raspel.erp.entity.finans.Banka b = new com.raspel.erp.entity.finans.Banka();
        b.setBakiye(BigDecimal.valueOf(40000));
        when(bankaRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of(b));

        Fatura fSatis = new Fatura();
        fSatis.setTur(Fatura.FaturaTur.SATIS);
        fSatis.setDurum(Fatura.FaturaDurum.KESILDI);
        fSatis.setGenelToplam(BigDecimal.valueOf(15000));
        fSatis.setVadeTarihi(LocalDate.now().plusDays(5));

        Fatura fAlis = new Fatura();
        fAlis.setTur(Fatura.FaturaTur.ALIS);
        fAlis.setDurum(Fatura.FaturaDurum.KESILDI);
        fAlis.setGenelToplam(BigDecimal.valueOf(5000));
        fAlis.setVadeTarihi(LocalDate.now().plusDays(10));

        when(faturaRepository.findBySirketIdOrderByTarihDesc(1L)).thenReturn(List.of(fSatis, fAlis));

        var result = raporService.nakitAkisiProjeksiyonu(30, 1L);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(50000), result.getBaslangicBakiyesi());
        assertEquals(BigDecimal.valueOf(15000), result.getToplamBeklenenGiris());
        assertEquals(BigDecimal.valueOf(5000), result.getToplamBeklenenCikis());
        assertEquals(BigDecimal.valueOf(60000), result.getTahminiBitisBakiyesi());
        assertEquals(31, result.getGunlukAkis().size());
    }
}