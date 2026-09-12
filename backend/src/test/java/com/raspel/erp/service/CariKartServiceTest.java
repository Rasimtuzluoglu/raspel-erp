package com.raspel.erp.service;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.CariKartDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.finans.CariFiyat;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.sistem.Not;
import com.raspel.erp.entity.ticaret.CariFirsat;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.Iade;
import com.raspel.erp.entity.ticaret.Siparis;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.CariFiyatRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.sistem.NotRepository;
import com.raspel.erp.repository.ticaret.CariFirsatRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.IadeRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.service.finans.CariKartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CariKartServiceTest {

    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private FaturaRepository faturaRepository;
    @Mock private SiparisRepository siparisRepository;
    @Mock private IadeRepository iadeRepository;
    @Mock private NotRepository notRepository;
    @Mock private CariFirsatRepository cariFirsatRepository;
    @Mock private CariFiyatRepository cariFiyatRepository;
    @Mock private HareketRepository hareketRepository;
    @Mock private StokRepository stokRepository;
    @Mock private com.raspel.erp.repository.finans.TaksitRepository taksitRepository;
    @Mock private TenantChecker tenantChecker;
    @InjectMocks private CariKartService cariKartService;

    private CariHesap cari(BigDecimal limit, BigDecimal bakiye) {
        return CariHesap.builder()
                .id(1L).ad("ABC Müşteri").telefon("555").email("a@b.com").tur("MUSTERI")
                .aktif(true).sirketId(1L).temsilciId(9L).temsilciAd("Temsilci X")
                .krediLimiti(limit).bakiye(bakiye)
                .build();
    }

    private void verileriHazirla(CariHesap cari) {
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));

        Fatura kesildi = Fatura.builder().id(10L).faturaNumarasi("F-10")
                .tarih(LocalDate.now()).tur(Fatura.FaturaTur.SATIS).durum(Fatura.FaturaDurum.KESILDI)
                .odemeDurumu("BEKLIYOR").genelToplam(new BigDecimal("5000")).kalanTutar(new BigDecimal("2000"))
                .sirketId(1L).build();
        Fatura iptal = Fatura.builder().id(11L).faturaNumarasi("F-11")
                .tarih(LocalDate.now().minusDays(2)).tur(Fatura.FaturaTur.SATIS).durum(Fatura.FaturaDurum.IPTAL)
                .genelToplam(new BigDecimal("1000")).kalanTutar(BigDecimal.ZERO).sirketId(1L).build();
        when(faturaRepository.findByCariHesapIdAndSirketIdOrderByTarihDesc(eq(1L), eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(kesildi, iptal)));

        when(siparisRepository.findByCariHesapId(1L)).thenReturn(List.of(
                Siparis.builder().id(20L).siparisNo("S-20").tarih(LocalDate.now())
                        .durum("TEKLIF").genelToplam(new BigDecimal("4000")).sirketId(1L).build()));

        when(iadeRepository.findByFaturaIdInAndSirketId(anyCollection(), eq(1L))).thenReturn(List.of(
                Iade.builder().id(30L).faturaId(10L).tur("SATIS").tarih(LocalDate.now())
                        .tutar(new BigDecimal("500")).durum("TAMAMLANDI").sirketId(1L).build()));

        when(notRepository.findByCariHesapIdOrderByOlusturmaTarihiDesc(1L)).thenReturn(List.of(
                Not.builder().id(40L).baslik("Not").icerik("Görüşüldü").onemDerecesi("NORMAL")
                        .olusturmaTarihi(LocalDateTime.now()).build()));

        when(cariFirsatRepository.findBySirketIdAndCariHesapIdOrderByOlusturmaTarihiDesc(1L, 1L)).thenReturn(List.of(
                CariFirsat.builder().id(50L).ad("Fırsat").durum("YENI").kaynak("WEB")
                        .deger(new BigDecimal("15000")).tahminiKapanis(LocalDate.now().plusDays(10)).build()));

        when(cariFiyatRepository.findByCariHesapIdOrderByStokId(1L)).thenReturn(List.of(
                CariFiyat.builder().id(60L).cariHesapId(1L).stokId(5L).fiyat(new BigDecimal("99"))
                        .sirketId(1L).build()));

        when(hareketRepository.findByCariHesapIdOrderByHareketTarihiDesc(1L)).thenReturn(List.of(
                Hareket.builder().id(70L).tur(Hareket.HareketTuru.TAHSILAT).tutar(new BigDecimal("1000")).build(),
                Hareket.builder().id(71L).tur(Hareket.HareketTuru.ODEME).tutar(new BigDecimal("200")).build()));

        when(stokRepository.findAllById(anyList())).thenReturn(List.of(
                Stok.builder().id(5L).ad("Ürün 5").stokKodu("K-5").build()));

        when(taksitRepository.findBySirketIdAndCariHesapIdOrderByVadeTarihiAsc(1L, 1L)).thenReturn(List.of(
                com.raspel.erp.entity.finans.Taksit.builder().id(80L).planNo("TKS-80")
                        .taksitNo(1).taksitSayisi(3).vadeTarihi(LocalDate.now().plusDays(10))
                        .tutar(new BigDecimal("1000")).odemeDurumu("BEKLEMEDE").build()));
    }

    @Test
    void kartGetir_ozetVeKrediHesaplar() {
        verileriHazirla(cari(new BigDecimal("10000"), new BigDecimal("3000")));

        CariKartDTO kart = cariKartService.kartGetir(1L, 1L);

        assertEquals("ABC Müşteri", kart.getCariAd());
        assertEquals("Temsilci X", kart.getTemsilciAd());

        assertEquals(new BigDecimal("7000"), kart.getKredi().getKullanilabilirKredi());
        assertEquals(new BigDecimal("30.00"), kart.getKredi().getRiskOrani());
        assertFalse(kart.getKredi().isLimitAsimi());

        assertEquals(1, kart.getOzet().getFaturaSayisi());
        assertEquals(new BigDecimal("5000"), kart.getOzet().getFaturaToplam());
        assertEquals(new BigDecimal("2000"), kart.getOzet().getKalanTutar());
        assertEquals(1, kart.getOzet().getSiparisSayisi());
        assertEquals(new BigDecimal("4000"), kart.getOzet().getSiparisToplam());
        assertEquals(1, kart.getOzet().getIadeSayisi());
        assertEquals(new BigDecimal("500"), kart.getOzet().getIadeToplam());
        assertEquals(new BigDecimal("1000"), kart.getOzet().getTahsilatToplam());

        assertEquals(2, kart.getSonFaturalar().size());
        assertEquals(1, kart.getFirsatlar().size());
        assertEquals(1, kart.getNotlar().size());
        assertEquals(1, kart.getOzelFiyatlar().size());
        assertEquals("Ürün 5", kart.getOzelFiyatlar().get(0).getStokAd());
        assertEquals(1, kart.getTaksitler().size());
        assertEquals("TKS-80", kart.getTaksitler().get(0).getPlanNo());
    }

    @Test
    void kartGetir_limitAsimiTespitEder() {
        verileriHazirla(cari(new BigDecimal("10000"), new BigDecimal("12000")));

        CariKartDTO kart = cariKartService.kartGetir(1L, 1L);

        assertTrue(kart.getKredi().isLimitAsimi());
        assertEquals(new BigDecimal("-2000"), kart.getKredi().getKullanilabilirKredi());
        assertEquals(new BigDecimal("120.00"), kart.getKredi().getRiskOrani());
    }

    @Test
    void kartGetir_bulunamazsaHataFirlatir() {
        when(cariHesapRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cariKartService.kartGetir(99L, 1L));
    }
}
