package com.raspel.erp.service;

import com.raspel.erp.dto.finans.TahsilatDTO;
import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.dto.finans.TahsilatHedefDTO;
import com.raspel.erp.dto.finans.TahsilatIslemDTO;
import com.raspel.erp.dto.finans.TahsilatPerformansDTO;
import com.raspel.erp.dto.finans.TopluHatirlatmaSonuc;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.entity.finans.PosTerminali;
import com.raspel.erp.entity.finans.TahsilatHedef;
import com.raspel.erp.entity.finans.TahsilatIslem;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.PosTerminaliRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.service.finans.HareketService;
import com.raspel.erp.service.finans.TahsilatService;
import com.raspel.erp.service.sistem.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TahsilatServiceTest {

    @Mock
    private FaturaRepository faturaRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private CariHesapRepository cariHesapRepository;
    @Mock
    private HareketService hareketService;
    @Mock
    private PosTerminaliRepository posTerminaliRepository;
    @Mock
    private com.raspel.erp.service.finans.TaksitService taksitService;
    @Mock
    private com.raspel.erp.service.muhasebe.OtomatikMuhasebeService otomatikMuhasebeService;
    @Mock
    private com.raspel.erp.service.sistem.DonemService donemService;
    @Mock
    private com.raspel.erp.repository.finans.HareketRepository hareketRepository;
    @Mock
    private com.raspel.erp.repository.finans.KasaRepository kasaRepository;
    @Mock
    private com.raspel.erp.repository.finans.KasaHareketRepository kasaHareketRepository;
    @Mock
    private com.raspel.erp.repository.finans.BankaRepository bankaRepository;
    @Mock
    private com.raspel.erp.repository.finans.BankaHareketiRepository bankaHareketiRepository;
    @Mock
    private com.raspel.erp.repository.finans.TahsilatIslemRepository tahsilatIslemRepository;
    @Mock
    private com.raspel.erp.repository.finans.TahsilatHedefRepository tahsilatHedefRepository;
    @Mock
    private com.raspel.erp.config.TenantChecker tenantChecker;
    @InjectMocks
    private TahsilatService tahsilatService;

    @BeforeEach
    void islemKaydiniHazirla() {
        lenient().when(tahsilatIslemRepository.save(any(TahsilatIslem.class))).thenAnswer(inv -> {
            TahsilatIslem islem = inv.getArgument(0);
            if (islem.getId() == null) islem.setId(42L);
            return islem;
        });
    }

    private CariHesap cari(Long id, String ad, String email) {
        return CariHesap.builder()
                .id(id).ad(ad).email(email).telefon("05555555555")
                .bakiye(BigDecimal.ZERO).aktif(true)
                .build();
    }

    private Fatura fatura(Long id, CariHesap cari, LocalDate vade, String tutar) {
        return Fatura.builder()
                .id(id)
                .faturaNumarasi("F-" + id)
                .tarih(vade.minusDays(30))
                .vadeTarihi(vade)
                .tur(Fatura.FaturaTur.SATIS)
                .durum(Fatura.FaturaDurum.KESILDI)
                .cariHesap(cari)
                .genelToplam(new BigDecimal(tutar))
                .kalanTutar(new BigDecimal(tutar))
                .sirketId(1L)
                .build();
    }

    @Test
    void ozetGetir_groupsByCariAndComputesAging() {
        CariHesap c1 = cari(1L, "A Ltd", "a@x.com");
        CariHesap c2 = cari(2L, "B Ltd", null);
        LocalDate bugun = LocalDate.now();
        List<Fatura> faturalar = List.of(
                fatura(1L, c1, bugun.minusDays(10), "1000"),
                fatura(2L, c1, bugun.plusDays(5), "2000"),
                fatura(3L, c2, bugun.minusDays(45), "3000"));

        when(faturaRepository.findTahsilatEdilecek(any(), any(), any(), anyList())).thenReturn(faturalar);

        TahsilatDTO ozet = tahsilatService.ozetGetir(1L);

        assertEquals(new BigDecimal("6000"), ozet.getToplamAlacak());
        assertEquals(3, ozet.getAcikFaturaSayisi());
        assertEquals(2, ozet.getCariler().size());
        assertEquals(new BigDecimal("4000"), ozet.getVadesiGecmisToplam());
    }

    @Test
    void ozetGetir_sortsByMaxDelayDesc() {
        CariHesap c1 = cari(1L, "Az Gecikmis", "a@x.com");
        CariHesap c2 = cari(2L, "Cok Gecikmis", "b@x.com");
        LocalDate bugun = LocalDate.now();
        List<Fatura> faturalar = List.of(
                fatura(1L, c1, bugun.minusDays(5), "1000"),
                fatura(2L, c2, bugun.minusDays(120), "2000"));

        when(faturaRepository.findTahsilatEdilecek(any(), any(), any(), anyList())).thenReturn(faturalar);

        TahsilatDTO ozet = tahsilatService.ozetGetir(1L);

        assertEquals("Cok Gecikmis", ozet.getCariler().get(0).getCariAd());
        assertEquals("90+ Gün", ozet.getCariler().get(0).getAralik());
    }

    @Test
    void hatirlat_sendsEmailForEachUnpaidFatura() {
        CariHesap c1 = cari(1L, "A Ltd", "a@x.com");
        List<Fatura> faturalar = List.of(
                fatura(1L, c1, LocalDate.now().minusDays(5), "1000"),
                fatura(2L, c1, LocalDate.now().minusDays(15), "2000"));

        when(faturaRepository.findTahsilatEdilecekByCari(any(), any(), any(), any(), anyList())).thenReturn(faturalar);
        when(emailService.odemeHatimlaticiGonder(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);

        int sent = tahsilatService.hatirlat(1L, 1L);

        assertEquals(2, sent);
        verify(emailService, times(2)).odemeHatimlaticiGonder(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void hatirlat_smtpYoksaHataVerir() {
        CariHesap c1 = cari(1L, "A Ltd", "a@x.com");
        List<Fatura> faturalar = List.of(
                fatura(1L, c1, LocalDate.now().minusDays(5), "1000"));

        when(faturaRepository.findTahsilatEdilecekByCari(any(), any(), any(), any(), anyList())).thenReturn(faturalar);
        when(emailService.odemeHatimlaticiGonder(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(false);

        assertThrows(BusinessException.class, () -> tahsilatService.hatirlat(1L, 1L));
    }

    @Test
    void hatirlat_throwsWhenNoEmail() {
        CariHesap c1 = cari(1L, "A Ltd", null);
        List<Fatura> faturalar = List.of(
                fatura(1L, c1, LocalDate.now().minusDays(5), "1000"));

        when(faturaRepository.findTahsilatEdilecekByCari(any(), any(), any(), any(), anyList())).thenReturn(faturalar);

        assertThrows(BusinessException.class, () -> tahsilatService.hatirlat(1L, 1L));
    }

    @Test
    void tahsilatGir_karttaPosAdiniCozer() {
        CariHesap c1 = cari(1L, "A Ltd", null);
        Fatura f = fatura(1L, c1, LocalDate.now().minusDays(5), "10000");
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(c1));
        when(faturaRepository.findTahsilatEdilecekByCari(any(), any(), any(), any(), anyList())).thenReturn(List.of(f));
        when(posTerminaliRepository.findById(9L)).thenReturn(Optional.of(PosTerminali.builder().id(9L).ad("Halkbank POS").build()));
        when(hareketService.hareketOlustur(any(), eq(1L))).thenReturn(null);

        tahsilatService.tahsilatGir(1L, new BigDecimal("10000"), "KART", null, null, null,
                LocalDate.now(), 1L, 9L, new BigDecimal("250"), LocalDate.now().plusDays(2));

        ArgumentCaptor<HareketDTO> captor = ArgumentCaptor.forClass(HareketDTO.class);
        verify(hareketService).hareketOlustur(captor.capture(), eq(1L));
        assertEquals(9L, captor.getValue().getPosTerminaliId());
        assertEquals("Halkbank POS", captor.getValue().getPosAd());
        assertEquals(new BigDecimal("250"), captor.getValue().getKomisyonTutar());
    }

    @Test
    void tahsilatGir_fazlaOdemeAlacakOlusturur() {
        CariHesap c1 = cari(1L, "A Ltd", null);
        Fatura f = fatura(1L, c1, LocalDate.now().minusDays(5), "1000");
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(c1));
        when(faturaRepository.findTahsilatEdilecekByCari(any(), any(), any(), any(), anyList())).thenReturn(List.of(f));
        when(hareketService.hareketOlustur(any(), eq(1L))).thenReturn(null);

        tahsilatService.tahsilatGir(1L, new BigDecimal("1500"), "NAKIT", null, null, null,
                LocalDate.now(), 1L, null, null, null);

        // 1 fatura tahsisi + 1 fazla ödeme (alacak) hareketi
        verify(hareketService, times(2)).hareketOlustur(any(), eq(1L));
    }

    @Test
    void tahsilatGir_taksitKaleminiOder() {
        CariHesap c1 = cari(1L, "A Ltd", null);
        Fatura f = fatura(1L, c1, LocalDate.now().minusDays(5), "1000");
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(c1));
        when(faturaRepository.findTahsilatEdilecekByCari(any(), any(), any(), any(), anyList())).thenReturn(List.of(f));
        when(hareketService.hareketOlustur(any(), eq(1L))).thenReturn(null);

        tahsilatService.tahsilatGir(1L, new BigDecimal("1000"), "TAKSIT", "Banka",
                new BigDecimal("1000"), null, LocalDate.now(), 1L, null, null, null, 55L);

        verify(taksitService).ode(eq(55L), any(com.raspel.erp.dto.finans.TaksitOdeDTO.class), eq(1L));
    }

    /**
     * Kilitli donemde tahsilat reddedilir ve hicbir yazma olmaz. Kontrol
     * hareketOlustur'da da var ama oradaki mesaj "hareket oluşturma" der;
     * kullanici ne yaptigini bilmez. Burada "tahsilat" adiyla ve erken
     * reddedilir.
     */
    @Test
    void tahsilatGir_kilitliDonemdeErkenReddeder() {
        LocalDate kilitliTarih = LocalDate.of(2026, 1, 15);
        doThrow(new BusinessException("Bu tarih kilitli bir döneme ait olduğu için tahsilat yapılamaz."))
                .when(donemService).kilitKontrol(eq(1L), eq(kilitliTarih), eq("tahsilat"));

        BusinessException hata = assertThrows(BusinessException.class, () ->
                tahsilatService.tahsilatGir(1L, new BigDecimal("1000"), "NAKIT", null, null, null,
                        kilitliTarih, 1L, null, null, null));

        assertTrue(hata.getMessage().contains("tahsilat"));
        verify(cariHesapRepository, never()).findById(any());
        verify(hareketService, never()).hareketOlustur(any(), any());
    }

    /** Tarih verilmezse kontrol bugun icin yapilir. */
    @Test
    void tahsilatGir_tarihYoksaBugunKontrolEdilir() {
        CariHesap c1 = cari(1L, "A Ltd", null);
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(c1));
        when(faturaRepository.findTahsilatEdilecekByCari(any(), any(), any(), any(), anyList())).thenReturn(List.of());
        when(hareketService.hareketOlustur(any(), eq(1L))).thenReturn(null);

        tahsilatService.tahsilatGir(1L, new BigDecimal("500"), "NAKIT", null, null, null,
                null, 1L, null, null, null);

        verify(donemService).kilitKontrol(eq(1L), eq(LocalDate.now()), eq("tahsilat"));
    }

    // ------------------------------------------------------------------
    // İşlem (batch) modeli
    // ------------------------------------------------------------------

    private TahsilatIslem islem(Long id, Long cariId, String durum, LocalDate tarih) {
        return TahsilatIslem.builder()
                .id(id)
                .sirketId(1L)
                .cariId(cariId)
                .tarih(tarih)
                .tutar(new BigDecimal("1000.00"))
                .odemeYontemi("NAKIT")
                .durum(durum)
                .build();
    }

    @Test
    void tahsilatGir_tekIslemOlustururVeTumHareketleriBaglar() {
        CariHesap c1 = cari(1L, "A Ltd", null);
        Fatura f1 = fatura(1L, c1, LocalDate.now().minusDays(10), "600");
        Fatura f2 = fatura(2L, c1, LocalDate.now().minusDays(5), "400");
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(c1));
        when(faturaRepository.findTahsilatEdilecekByCari(any(), any(), any(), any(), anyList()))
                .thenReturn(List.of(f1, f2));
        when(hareketService.hareketOlustur(any(), eq(1L)))
                .thenReturn(HareketDTO.builder().id(100L).build(),
                        HareketDTO.builder().id(101L).build());

        Map<String, Object> sonuc = tahsilatService.tahsilatGir(
                1L, new BigDecimal("1000"), "NAKIT", null, null, null,
                LocalDate.now(), 1L, null, null, null);
        // Overload zinciri kullanıcı id'sini de kabul eder.
        Map<String, Object> sonuc2 = tahsilatService.tahsilatGir(
                1L, new BigDecimal("1000"), "NAKIT", null, null, null,
                LocalDate.now(), 1L, null, null, null, null, null, null, 9L);

        assertEquals(42L, sonuc.get("tahsilatIslemId"));
        assertEquals(42L, sonuc2.get("tahsilatIslemId"));

        ArgumentCaptor<HareketDTO> captor = ArgumentCaptor.forClass(HareketDTO.class);
        verify(hareketService, atLeast(2)).hareketOlustur(captor.capture(), eq(1L));
        assertTrue(captor.getAllValues().stream()
                .allMatch(d -> Long.valueOf(42L).equals(d.getTahsilatIslemId())));
        verify(tahsilatIslemRepository, atLeastOnce()).save(argThat(i ->
                TahsilatIslem.DURUM_AKTIF.equals(i.getDurum())
                        && new BigDecimal("1000").compareTo(i.getTutar()) == 0));
    }

    @Test
    void iptal_cariFaturaKasaBankaBakiyeleriniGeriAlir() {
        LocalDate tarih = LocalDate.now().minusDays(3);
        TahsilatIslem islem = islem(5L, 1L, TahsilatIslem.DURUM_AKTIF, tarih);
        when(tahsilatIslemRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(islem));
        when(hareketRepository.findIdByTahsilatIslemId(5L)).thenReturn(List.of(11L, 12L));
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari(1L, "A Ltd", null)));

        TahsilatIslemDTO dto = tahsilatService.iptalEt(5L, "müşteri vazgeçti", 9L);

        // Hareket iptali bakiyeleri (cari/fatura/kasa/banka) kendi servisinde ters kaydeder.
        verify(hareketService).hareketIptal(11L);
        verify(hareketService).hareketIptal(12L);
        assertEquals(TahsilatIslem.DURUM_IPTAL, dto.getDurum());
        assertEquals("müşteri vazgeçti", dto.getIptalGerekce());
        assertEquals(TahsilatIslem.DURUM_IPTAL, islem.getDurum());
        assertEquals(9L, islem.getIptalEdenKullaniciId());
        assertNotNull(islem.getIptalTarihi());
    }

    @Test
    void iptal_ikinciKezReddeder() {
        TahsilatIslem islem = islem(5L, 1L, TahsilatIslem.DURUM_IPTAL, LocalDate.now());
        when(tahsilatIslemRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(islem));

        BusinessException hata = assertThrows(BusinessException.class,
                () -> tahsilatService.iptalEt(5L, "tekrar iptal", 9L));

        assertTrue(hata.getMessage().contains("zaten iptal"));
        verify(hareketService, never()).hareketIptal(any());
    }

    @Test
    void iptal_donemKilitliyseReddeder() {
        LocalDate tarih = LocalDate.of(2026, 1, 15);
        TahsilatIslem islem = islem(5L, 1L, TahsilatIslem.DURUM_AKTIF, tarih);
        when(tahsilatIslemRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(islem));
        doThrow(new BusinessException("Bu dönem kilitli"))
                .when(donemService).kilitKontrol(eq(1L), eq(tarih), eq("tahsilat iptali"));

        assertThrows(BusinessException.class, () -> tahsilatService.iptalEt(5L, "dönem kilitli", 9L));

        verify(hareketService, never()).hareketIptal(any());
        assertEquals(TahsilatIslem.DURUM_AKTIF, islem.getDurum());
    }

    @Test
    void gecmisFiltreleriVeDagilimlariDoner() {
        LocalDate tarih = LocalDate.now().minusDays(2);
        TahsilatIslem islem = islem(5L, 3L, TahsilatIslem.DURUM_AKTIF, tarih);
        islem.setKasaId(4L);
        islem.setLegacyHareketId(null);

        when(tahsilatIslemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(islem)));
        when(cariHesapRepository.findAllById(any()))
                .thenReturn(List.of(cari(3L, "Acme Ltd", "a@x.com")));
        when(kasaRepository.findAllById(any()))
                .thenReturn(List.of(Kasa.builder().id(4L).ad("Merkez Kasa").build()));
        when(hareketRepository.tahsilatDagilimlariniGetir(any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{5L, 77L, 8L, "F-8", new BigDecimal("100.00"), "Tahsilat: F-8", false},
                        new Object[]{5L, 78L, null, null, new BigDecimal("50.00"), "Avans tahsilat", true}));

        Page<TahsilatIslemDTO> page = tahsilatService.gecmisFiltreli(
                1L, "acme", 3L, "NAKIT", "AKTIF", 4L, null, tarih.minusDays(1), tarih.plusDays(1),
                PageRequest.of(0, 25));

        assertEquals(1, page.getTotalElements());
        TahsilatIslemDTO dto = page.getContent().get(0);
        assertEquals("Acme Ltd", dto.getCariAd());
        assertEquals("Merkez Kasa", dto.getKasaAd());
        assertEquals(2, dto.getSatirSayisi());
        assertEquals(2, dto.getDagilimlar().size());
        assertEquals("F-8", dto.getDagilimlar().get(0).getFaturaNumarasi());
        // İptal edilmiş dağıtım da döner (geçmiş görünürlüğü).
        assertTrue(dto.getDagilimlar().get(1).isIptal());
    }

    @Test
    void gecmis_legacyTekSatirIslemDoner() {
        TahsilatIslem legacy = islem(8L, 2L, TahsilatIslem.DURUM_AKTIF, LocalDate.now().minusMonths(2));
        legacy.setLegacyHareketId(99L);
        when(tahsilatIslemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(legacy)));
        when(cariHesapRepository.findAllById(any())).thenReturn(List.of(cari(2L, "Eski Müşteri", null)));
        when(hareketRepository.tahsilatDagilimlariniGetir(any()))
                .thenReturn(List.<Object[]>of(new Object[]{8L, 99L, null, null,
                        new BigDecimal("1000.00"), "Legacy tahsilat", false}));

        Page<TahsilatIslemDTO> page = tahsilatService.gecmisFiltreli(
                1L, null, null, null, null, null, null, null, null, PageRequest.of(0, 25));

        assertEquals(8L, page.getContent().get(0).getId());
        assertEquals(99L, page.getContent().get(0).getDagilimlar().get(0).getHareketId());
    }

    // ------------------------------------------------------------------
    // Hedef / performans
    // ------------------------------------------------------------------

    @Test
    void performans_hedefeGoreGerceklesmeHesaplar() {
        LocalDate bugun = LocalDate.now();
        CariHesap c1 = cari(1L, "A Ltd", null);
        Fatura gecmisFatura = fatura(1L, c1, bugun.minusDays(10), "200");
        when(tahsilatHedefRepository.findBySirketIdAndYilAndAy(1L, 2026, 10))
                .thenReturn(Optional.of(TahsilatHedef.builder()
                        .id(1L).sirketId(1L).yil(2026).ay(10).tutar(new BigDecimal("1000.00")).build()));
        when(tahsilatIslemRepository.aktifTahsilatToplami(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("750.00"));
        when(tahsilatIslemRepository.aktifTahsilatSayisi(eq(1L), any(), any())).thenReturn(3L);
        when(faturaRepository.findTahsilatEdilecek(any(), any(), any(), anyList()))
                .thenReturn(List.of(gecmisFatura));

        TahsilatPerformansDTO dto = tahsilatService.performans(1L, 2026, 10);

        assertEquals(new BigDecimal("1000.00"), dto.getHedef());
        assertEquals(new BigDecimal("750.00"), dto.getTahsilEdilen());
        assertEquals(new BigDecimal("75.00"), dto.getGerceklesmeYuzde());
        assertEquals(3, dto.getTahsilatSayisi());
        assertEquals(new BigDecimal("200"), dto.getVadesiGecmisToplam());
        assertEquals(6, dto.getTrend().size());
        assertEquals("2026-10", dto.getTrend().get(5).getAy());
    }

    @Test
    void performans_hedefYoksaNullDoner() {
        when(tahsilatHedefRepository.findBySirketIdAndYilAndAy(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(tahsilatIslemRepository.aktifTahsilatToplami(eq(1L), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(tahsilatIslemRepository.aktifTahsilatSayisi(eq(1L), any(), any())).thenReturn(0L);
        when(faturaRepository.findTahsilatEdilecek(any(), any(), any(), anyList()))
                .thenReturn(List.of());

        TahsilatPerformansDTO dto = tahsilatService.performans(1L, 2026, 10);

        assertNull(dto.getHedef());
        assertNull(dto.getGerceklesmeYuzde());
        assertEquals(0, dto.getTahsilatSayisi());
        assertEquals(6, dto.getTrend().size());
    }

    @Test
    void hedef_upsertEder() {
        TahsilatHedef mevcut = TahsilatHedef.builder()
                .id(7L).sirketId(1L).yil(2026).ay(10)
                .tutar(new BigDecimal("100.00")).olusturanKullaniciId(2L).build();
        when(tahsilatHedefRepository.findBySirketIdAndYilAndAy(1L, 2026, 10))
                .thenReturn(Optional.of(mevcut));
        when(tahsilatHedefRepository.save(any(TahsilatHedef.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TahsilatHedefDTO dto = tahsilatService.hedefKaydet(
                1L, 2026, 10, new BigDecimal("500.00"), 9L);

        assertEquals(7L, mevcut.getId());
        assertEquals(new BigDecimal("500.00"), dto.getTutar());
        assertEquals(new BigDecimal("500.00"), mevcut.getTutar());
        assertNotNull(mevcut.getGuncellemeTarihi());
        verify(tahsilatHedefRepository).save(mevcut);
    }

    @Test
    void hedef_negatifTutarReddeder() {
        assertThrows(BusinessException.class,
                () -> tahsilatService.hedefKaydet(1L, 2026, 10, new BigDecimal("-1"), 9L));
        verify(tahsilatHedefRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Yaşlandırma filtreleri + toplu hatırlatma
    // ------------------------------------------------------------------

    @Test
    void ozetFiltreleriUygular() {
        CariHesap c1 = cari(1L, "Acme Ltd", "a@x.com");
        CariHesap c2 = cari(2L, "Beta Ltd", "b@x.com");
        LocalDate bugun = LocalDate.now();
        when(faturaRepository.findTahsilatEdilecek(any(), any(), any(), anyList()))
                .thenReturn(List.of(
                        fatura(1L, c1, bugun.minusDays(10), "1000"),
                        fatura(2L, c2, bugun.plusDays(5), "2000")));

        TahsilatDTO dto = tahsilatService.ozetGetir(1L, "acme", null, null, null);

        assertEquals(1, dto.getCariler().size());
        assertEquals("Acme Ltd", dto.getCariler().get(0).getCariAd());
        // Özet toplamları global kalır (filtreden etkilenmez).
        assertEquals(new BigDecimal("3000"), dto.getToplamAlacak());

        TahsilatDTO gecikmis = tahsilatService.ozetGetir(
                1L, null, "31-60", true, new BigDecimal("500"));
        assertEquals(0, gecikmis.getCariler().size());
    }

    @Test
    void topluHatirlatma_epostasizlariAtlar() {
        CariHesap epostali = cari(1L, "A Ltd", "a@x.com");
        CariHesap epostasiz = cari(2L, "B Ltd", null);
        when(cariHesapRepository.findBySirketIdAndIdIn(eq(1L), any()))
                .thenReturn(List.of(epostali, epostasiz));
        when(faturaRepository.findTahsilatEdilecekByCari(eq(1L), eq(1L), any(), any(), anyList()))
                .thenReturn(List.of(fatura(1L, epostali, LocalDate.now().minusDays(3), "1000")));
        when(emailService.odemeHatimlaticiGonder(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(true);

        TopluHatirlatmaSonuc sonuc = tahsilatService.topluHatirlat(1L, List.of(1L, 2L));

        assertEquals(1, sonuc.getGonderilen());
        assertEquals(1, sonuc.getAtlanan());
        assertTrue(sonuc.getHatalar().isEmpty());
        verify(emailService, times(1)).odemeHatimlaticiGonder(anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void topluHatirlatma_gonderimHatasiniRaporlar() {
        CariHesap c1 = cari(1L, "A Ltd", "a@x.com");
        when(cariHesapRepository.findBySirketIdAndIdIn(eq(1L), any())).thenReturn(List.of(c1));
        when(faturaRepository.findTahsilatEdilecekByCari(eq(1L), eq(1L), any(), any(), anyList()))
                .thenReturn(List.of(fatura(1L, c1, LocalDate.now().minusDays(3), "1000")));
        when(emailService.odemeHatimlaticiGonder(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(false);

        TopluHatirlatmaSonuc sonuc = tahsilatService.topluHatirlat(1L, List.of(1L));

        assertEquals(0, sonuc.getGonderilen());
        assertEquals(1, sonuc.getAtlanan());
        assertEquals(1, sonuc.getHatalar().size());
    }

    @Test
    void topluHatirlatma_bosSecimReddeder() {
        assertThrows(BusinessException.class, () -> tahsilatService.topluHatirlat(1L, List.of()));
    }
}