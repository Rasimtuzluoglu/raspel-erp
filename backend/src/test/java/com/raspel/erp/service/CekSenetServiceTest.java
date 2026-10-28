package com.raspel.erp.service;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.CekSenetDTO;
import com.raspel.erp.dto.finans.CekSenetOzetDTO;
import com.raspel.erp.dto.finans.CekSenetTakvimDTO;
import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.BankaHareketi;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.CekSenet;
import com.raspel.erp.entity.finans.CekSenetDurumLog;
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.entity.finans.KasaHareket;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.BankaHareketiRepository;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.CekSenetDurumLogRepository;
import com.raspel.erp.repository.finans.CekSenetRepository;
import com.raspel.erp.repository.finans.KasaHareketRepository;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.service.finans.CekSenetService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CekSenetServiceTest {

    @Mock private CekSenetRepository cekSenetRepository;
    @Mock private CekSenetDurumLogRepository cekSenetDurumLogRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private FaturaRepository faturaRepository;
    @Mock private TenantChecker tenantChecker;
    @Mock private com.raspel.erp.service.finans.HareketService hareketService;
    @Mock private KasaRepository kasaRepository;
    @Mock private KasaHareketRepository kasaHareketRepository;
    @Mock private BankaRepository bankaRepository;
    @Mock private BankaHareketiRepository bankaHareketiRepository;
    @Mock private com.raspel.erp.service.sistem.DonemService donemService;
    @Mock private com.raspel.erp.service.muhasebe.OtomatikMuhasebeService otomatikMuhasebeService;
    @InjectMocks private CekSenetService cekSenetService;

    // ------------------------------------------------------------------
    // Yardımcılar
    // ------------------------------------------------------------------

    private CekSenet createCekSenet(Long id) {
        CekSenet cs = new CekSenet();
        cs.setId(id);
        cs.setTur("CEK");
        cs.setCariHesapId(1L);
        cs.setBankaAdi("Is Bankasi");
        cs.setCekNo("12345");
        cs.setVadeTarihi(LocalDate.now().plusMonths(3));
        cs.setTutar(BigDecimal.valueOf(10000));
        cs.setDurum("PORTFOY");
        cs.setSirketId(1L);
        cs.setOlusturmaTarihi(LocalDateTime.now());
        return cs;
    }

    private void kaydetDonusu() {
        when(cekSenetRepository.save(any(CekSenet.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private CariHesap cari(Long id, String ad, Long sirketId) {
        return CariHesap.builder().id(id).ad(ad).sirketId(sirketId).build();
    }

    // ------------------------------------------------------------------
    // Okuma
    // ------------------------------------------------------------------

    @Test
    void tumunuGetir_returnsAll() {
        when(cekSenetRepository.findBySirketIdOrderByVadeTarihiAsc(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(createCekSenet(1L))));
        var result = cekSenetService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getir_returnsCekSenet() {
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(createCekSenet(1L)));
        var result = cekSenetService.getir(1L);
        assertEquals("CEK", result.getTur());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(cekSenetRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cekSenetService.getir(99L));
    }

    @Test
    void getir_legacyOdendiTahsilEdildiyeEslenir() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("ODENDI");
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));

        var result = cekSenetService.getir(1L);

        assertEquals("TAHSIL_EDILDI", result.getDurum());
    }

    @Test
    void getir_gecmisDondurur() {
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(createCekSenet(1L)));
        when(cekSenetDurumLogRepository.findByCekSenetIdOrderByOlusturmaTarihiAscIdAsc(1L))
                .thenReturn(List.of(CekSenetDurumLog.builder()
                        .id(9L).cekSenetId(1L).islem("OLUSTUR").yeniDurum("PORTFOY")
                        .olusturmaTarihi(LocalDateTime.now()).build()));

        var result = cekSenetService.getir(1L);

        assertEquals(1, result.getGecmis().size());
        assertEquals("OLUSTUR", result.getGecmis().get(0).getIslem());
    }

    @Test
    void olustur_createsAndLogs() {
        CekSenetDTO dto = CekSenetDTO.builder().tur("SENET").cariHesapId(1L)
                .vadeTarihi(LocalDate.now().plusMonths(2)).tutar(BigDecimal.valueOf(5000)).sirketId(1L).build();
        CekSenet saved = createCekSenet(1L);
        saved.setTur("SENET");
        when(cekSenetRepository.save(any(CekSenet.class))).thenReturn(saved);

        var result = cekSenetService.olustur(dto);

        assertEquals("SENET", result.getTur());
        assertEquals("PORTFOY", result.getDurum());
        verify(cekSenetDurumLogRepository).save(argThat(l -> "OLUSTUR".equals(l.getIslem())
                && "PORTFOY".equals(l.getYeniDurum())
                && Long.valueOf(1L).equals(l.getSirketId())));
    }

    // ------------------------------------------------------------------
    // Tahsil
    // ------------------------------------------------------------------

    @Test
    void durumGuncelle_tahsilHareketIdleriKaydedilir() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        kaydetDonusu();
        when(hareketService.hareketOlustur(any(com.raspel.erp.dto.finans.HareketDTO.class), eq(1L)))
                .thenReturn(HareketDTO.builder().id(501L).build());
        Kasa kasa = Kasa.builder().id(10L).ad("Merkez").bakiye(BigDecimal.valueOf(1000)).sirketId(1L).build();
        when(kasaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(kasa));
        when(kasaHareketRepository.save(any(KasaHareket.class)))
                .thenAnswer(inv -> {
                    KasaHareket kh = inv.getArgument(0);
                    kh.setId(601L);
                    return kh;
                });

        var result = cekSenetService.durumGuncelle(1L, "TAHSIL_EDILDI", 10L, null);

        assertEquals("TAHSIL_EDILDI", result.getDurum());
        assertEquals(501L, cs.getTahsilatHareketId().longValue());
        assertEquals(601L, cs.getKasaHareketId().longValue());
        assertNull(cs.getBankaHareketiId());
        assertEquals(0, BigDecimal.valueOf(11000).compareTo(kasa.getBakiye()));
        verify(kasaHareketRepository).save(argThat(kh -> "CEK_SENET".equals(kh.getKaynakTip())
                && Long.valueOf(1L).equals(kh.getKaynakId())));
        verify(otomatikMuhasebeService).cekSenetTahsilIsle(cs, 10L, null);
        verify(cekSenetDurumLogRepository).save(argThat(l -> "TAHSIL".equals(l.getIslem())
                && "PORTFOY".equals(l.getEskiDurum()) && "TAHSIL_EDILDI".equals(l.getYeniDurum())));
    }

    @Test
    void durumGuncelle_tahsilBankaHareketiBagi() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        kaydetDonusu();
        when(hareketService.hareketOlustur(any(com.raspel.erp.dto.finans.HareketDTO.class), eq(1L)))
                .thenReturn(HareketDTO.builder().id(502L).build());
        Banka banka = Banka.builder().id(20L).ad("Ziraat").bakiye(BigDecimal.ZERO).sirketId(1L).build();
        when(bankaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(banka));
        when(bankaHareketiRepository.save(any(BankaHareketi.class)))
                .thenAnswer(inv -> {
                    BankaHareketi bh = inv.getArgument(0);
                    bh.setId(701L);
                    return bh;
                });

        cekSenetService.durumGuncelle(1L, "TAHSIL_EDILDI", null, 20L);

        assertEquals(502L, cs.getTahsilatHareketId().longValue());
        assertEquals(701L, cs.getBankaHareketiId().longValue());
        assertNull(cs.getKasaHareketId());
        verify(bankaHareketiRepository).save(argThat(bh -> "CEK_SENET".equals(bh.getKaynakTip())
                && Long.valueOf(1L).equals(bh.getKaynakId())));
        verify(otomatikMuhasebeService).cekSenetTahsilIsle(cs, null, 20L);
    }

    @Test
    void durumGuncelle_tahsilKasaVeBankaBirlikteReddedilir() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));

        var ex = assertThrows(BusinessException.class,
                () -> cekSenetService.durumGuncelle(1L, "TAHSIL_EDILDI", 10L, 20L));

        assertTrue(ex.getMessage().contains("hem kasaya hem bankaya"));
        verifyNoInteractions(hareketService);
        verify(cekSenetRepository, never()).save(any());
    }

    @Test
    void durumGuncelle_tahsilFaturaBaskaCariyeAitOlamaz() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        Fatura fatura = Fatura.builder().id(77L).sirketId(1L)
                .cariHesap(cari(2L, "Baska Cari", 1L)).build();
        when(faturaRepository.findById(77L)).thenReturn(Optional.of(fatura));

        var ex = assertThrows(BusinessException.class,
                () -> cekSenetService.durumGuncelle(1L, "TAHSIL_EDILDI", null, null, 77L));

        assertTrue(ex.getMessage().contains("Fatura bu cari hesaba ait değil"));
        verifyNoInteractions(hareketService);
    }

    @Test
    void durumGuncelle_tahsilFaturaCariyeAitseHareketeBaglanir() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        kaydetDonusu();
        Fatura fatura = Fatura.builder().id(77L).sirketId(1L)
                .cariHesap(cari(1L, "Musteri", 1L)).build();
        when(faturaRepository.findById(77L)).thenReturn(Optional.of(fatura));
        when(hareketService.hareketOlustur(any(com.raspel.erp.dto.finans.HareketDTO.class), eq(1L)))
                .thenReturn(HareketDTO.builder().id(503L).build());

        cekSenetService.durumGuncelle(1L, "TAHSIL_EDILDI", null, null, 77L);

        verify(hareketService).hareketOlustur(argThat(d -> Long.valueOf(77L).equals(d.getFaturaId())), eq(1L));
        assertEquals(503L, cs.getTahsilatHareketId().longValue());
    }

    @Test
    void durumGuncelle_tahsilKayitliFaturayiKullanir() {
        CekSenet cs = createCekSenet(1L);
        cs.setFaturaId(77L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        kaydetDonusu();
        Fatura fatura = Fatura.builder().id(77L).sirketId(1L)
                .cariHesap(cari(1L, "Musteri", 1L)).build();
        when(faturaRepository.findById(77L)).thenReturn(Optional.of(fatura));
        when(hareketService.hareketOlustur(any(com.raspel.erp.dto.finans.HareketDTO.class), eq(1L)))
                .thenReturn(HareketDTO.builder().id(504L).build());

        cekSenetService.durumGuncelle(1L, "TAHSIL_EDILDI");

        verify(hareketService).hareketOlustur(argThat(d -> Long.valueOf(77L).equals(d.getFaturaId())), eq(1L));
    }

    @Test
    void durumGuncelle_tahsilEdilmisGeriAlinamaz() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("TAHSIL_EDILDI");
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));

        assertThrows(BusinessException.class, () -> cekSenetService.durumGuncelle(1L, "PORTFOY"));
        verifyNoInteractions(hareketService);
    }

    @Test
    void durumGuncelle_gecersizGecisReddedilir() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("PROTESTO");
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));

        var ex = assertThrows(BusinessException.class,
                () -> cekSenetService.durumGuncelle(1L, "PORTFOY"));

        assertTrue(ex.getMessage().contains("Geçersiz durum geçişi"));
    }

    @Test
    void durumGuncelle_throwsWhenNotFound() {
        when(cekSenetRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> cekSenetService.durumGuncelle(99L, "TAHSIL_EDILDI"));
    }

    @Test
    void durumGuncelle_kilitliDonem_reddedilir() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        doThrow(new BusinessException("Bu tarih kilitli"))
                .when(donemService).kilitKontrol(any(), any(), anyString());

        assertThrows(BusinessException.class,
                () -> cekSenetService.durumGuncelle(1L, "TAHSIL_EDILDI"));
        verifyNoInteractions(hareketService);
    }

    // ------------------------------------------------------------------
    // Tahsil iptali
    // ------------------------------------------------------------------

    @Test
    void tahsilIptal_tersKayitYaparVePortfoyeDoner() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("TAHSIL_EDILDI");
        cs.setTahsilatHareketId(501L);
        cs.setKasaHareketId(601L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        kaydetDonusu();
        Kasa kasa = Kasa.builder().id(10L).ad("Merkez").bakiye(BigDecimal.valueOf(11000)).sirketId(1L).build();
        KasaHareket kh = KasaHareket.builder().id(601L).kasa(kasa).tur("GELIR")
                .tutar(BigDecimal.valueOf(10000)).build();
        when(kasaHareketRepository.findById(601L)).thenReturn(Optional.of(kh));

        var result = cekSenetService.tahsilIptal(1L, "Hatalı tahsil");

        assertEquals("PORTFOY", result.getDurum());
        assertNull(cs.getTahsilatHareketId());
        assertNull(cs.getKasaHareketId());
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(kasa.getBakiye()));
        verify(hareketService).hareketIptal(501L);
        verify(kasaHareketRepository).delete(kh);
        verify(cekSenetDurumLogRepository).save(argThat(l -> "TAHSIL_IPTAL".equals(l.getIslem())
                && "Hatalı tahsil".equals(l.getAciklama())));
    }

    @Test
    void tahsilIptal_bankaBakiyesiDuzeltilir() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("TAHSIL_EDILDI");
        cs.setBankaHareketiId(701L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        kaydetDonusu();
        Banka banka = Banka.builder().id(20L).ad("Ziraat").bakiye(BigDecimal.valueOf(15000)).sirketId(1L).build();
        BankaHareketi bh = BankaHareketi.builder().id(701L).bankaId(20L).alacak(BigDecimal.valueOf(5000)).build();
        when(bankaHareketiRepository.findById(701L)).thenReturn(Optional.of(bh));
        when(bankaRepository.findById(20L)).thenReturn(Optional.of(banka));

        cekSenetService.tahsilIptal(1L, "Banka iadesi");

        assertEquals(0, BigDecimal.valueOf(10000).compareTo(banka.getBakiye()));
        verify(bankaHareketiRepository).delete(bh);
        verify(hareketService, never()).hareketIptal(any());
    }

    @Test
    void tahsilIptal_gerekceZorunlu() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("TAHSIL_EDILDI");
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));

        assertThrows(BusinessException.class, () -> cekSenetService.tahsilIptal(1L, null));
        assertThrows(BusinessException.class, () -> cekSenetService.tahsilIptal(1L, "ab"));
        verifyNoInteractions(hareketService);
    }

    @Test
    void tahsilIptal_ikinciIptalEngellenir() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("TAHSIL_EDILDI");
        cs.setTahsilatHareketId(501L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        kaydetDonusu();

        cekSenetService.tahsilIptal(1L, "İlk iptal");

        assertThrows(BusinessException.class, () -> cekSenetService.tahsilIptal(1L, "İkinci iptal"));
    }

    @Test
    void tahsilIptal_hareketBagiYoksaReddedilir() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("TAHSIL_EDILDI");
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));

        var ex = assertThrows(BusinessException.class,
                () -> cekSenetService.tahsilIptal(1L, "Eski kayıt iptali"));

        assertTrue(ex.getMessage().contains("hareket bağlantısı"));
        verifyNoInteractions(hareketService);
    }

    @Test
    void tahsilIptal_hareketBaskaEkrandanIptalEdilmisseDevamEder() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("TAHSIL_EDILDI");
        cs.setTahsilatHareketId(501L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        kaydetDonusu();
        when(hareketService.hareketIptal(501L)).thenThrow(new ResourceNotFoundException("Hareket", 501L));

        var result = cekSenetService.tahsilIptal(1L, "Hareket zaten iptal");

        assertEquals("PORTFOY", result.getDurum());
    }

    @Test
    void tahsilIptal_tahsilEdilmemisKayitReddedilir() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));

        assertThrows(BusinessException.class, () -> cekSenetService.tahsilIptal(1L, "Geçersiz"));
    }

    @Test
    void tahsilIptal_kilitliDonemReddedilir() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("TAHSIL_EDILDI");
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        doThrow(new BusinessException("Bu tarih kilitli"))
                .when(donemService).kilitKontrol(any(), any(), anyString());

        assertThrows(BusinessException.class, () -> cekSenetService.tahsilIptal(1L, "Kilitli dönem"));
        verifyNoInteractions(hareketService);
    }

    // ------------------------------------------------------------------
    // Ciro / teminat / protesto
    // ------------------------------------------------------------------

    @Test
    void ciroEt_cariyeCiroEderFinansalHareketYok() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        kaydetDonusu();
        when(cariHesapRepository.findById(5L)).thenReturn(Optional.of(cari(5L, "Tedarikci", 1L)));

        var result = cekSenetService.ciroEt(1L, 5L, "Borca karşılık");

        assertEquals("CIRO_EDILDI", result.getDurum());
        assertEquals(5L, cs.getCiroCariId().longValue());
        assertEquals("Tedarikci", result.getCiroCariAd());
        assertNotNull(cs.getCiroTarihi());
        assertEquals("Borca karşılık", cs.getCiroNotu());
        verifyNoInteractions(hareketService);
        verify(cekSenetDurumLogRepository).save(argThat(l -> "CIRO".equals(l.getIslem())));
    }

    @Test
    void ciroEt_cariYoksaReddedilir() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));

        assertThrows(BusinessException.class, () -> cekSenetService.ciroEt(1L, null, null));
    }

    @Test
    void ciroEt_baskaSirketinCarisiReddedilir() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        when(cariHesapRepository.findById(6L)).thenReturn(Optional.of(cari(6L, "Yabanci", 2L)));

        assertThrows(BusinessException.class, () -> cekSenetService.ciroEt(1L, 6L, null));
    }

    @Test
    void ciroEt_portfoyDisiReddedilir() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("TEMINATTA");
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));

        assertThrows(BusinessException.class, () -> cekSenetService.ciroEt(1L, 5L, null));
    }

    @Test
    void ciroIade_ve_iadeAl() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("CIRO_EDILDI");
        cs.setCiroCariId(5L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        kaydetDonusu();

        assertEquals("IADE", cekSenetService.ciroIade(1L).getDurum());
        var portfoy = cekSenetService.iadeAl(1L);

        assertEquals("PORTFOY", portfoy.getDurum());
        assertNull(portfoy.getCiroCariId());
    }

    @Test
    void ciroIade_portfoydekiKayittaReddedilir() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));

        assertThrows(BusinessException.class, () -> cekSenetService.ciroIade(1L));
    }

    @Test
    void teminatVer_ve_teminatIade() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        kaydetDonusu();

        var teminatta = cekSenetService.teminatVer(1L, "Banka teminat mektubu");

        assertEquals("TEMINATTA", teminatta.getDurum());
        assertNotNull(teminatta.getTeminatTarihi());
        assertEquals("Banka teminat mektubu", teminatta.getTeminatAciklama());

        var portfoy = cekSenetService.teminatIade(1L);

        assertEquals("PORTFOY", portfoy.getDurum());
        assertNull(portfoy.getTeminatTarihi());
        assertNull(portfoy.getTeminatAciklama());
        verify(cekSenetDurumLogRepository).save(argThat(l -> "TEMINAT_IADE".equals(l.getIslem())));
    }

    @Test
    void protesto_terminalDurumdur() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        kaydetDonusu();

        var result = cekSenetService.protesto(1L, "Karşılıksız");

        assertEquals("PROTESTO", result.getDurum());
        assertNotNull(result.getProtestoTarihi());
        assertThrows(BusinessException.class, () -> cekSenetService.durumGuncelle(1L, "PORTFOY"));
        assertThrows(BusinessException.class, () -> cekSenetService.teminatVer(1L, null));
    }

    @Test
    void teminattanProtestoEdilebilir() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("TEMINATTA");
        cs.setTeminatTarihi(LocalDateTime.now());
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        kaydetDonusu();

        assertEquals("PROTESTO", cekSenetService.protesto(1L, null).getDurum());
    }

    // ------------------------------------------------------------------
    // Filtre / özet / takvim
    // ------------------------------------------------------------------

    @Test
    void filtreli_legacyOdendiDurumunuTahsilFiltresineCevirir() {
        when(cekSenetRepository.filtreliGetir(eq(1L), isNull(), eq("CEK"), eq("TAHSIL_EDILDI"),
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createCekSenet(1L))));

        var result = cekSenetService.filtreli(1L, null, "cek", "odendi", null,
                null, null, null, null, null, Pageable.unpaged());

        assertEquals(1, result.getContent().size());
    }

    @Test
    void filtreli_gecikenAraligiBitisiDuneCevirir() {
        LocalDate bugun = LocalDate.now();
        when(cekSenetRepository.filtreliGetir(eq(1L), isNull(), isNull(), isNull(),
                isNull(), isNull(), eq(bugun.minusDays(1)), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = cekSenetService.filtreli(1L, null, null, null, null,
                null, null, "GECIKEN", null, null, Pageable.unpaged());

        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void filtreli_gecersizVadeAraligiReddedilir() {
        assertThrows(BusinessException.class, () -> cekSenetService.filtreli(1L, null, null, null, null,
                null, null, "SAÇMA", null, null, Pageable.unpaged()));
    }

    @Test
    void ozet_sayilariToplar() {
        when(cekSenetRepository.countDurum(1L, "PORTFOY", null, null)).thenReturn(3L);
        when(cekSenetRepository.countDurum(1L, "TEMINATTA", null, null)).thenReturn(2L);
        when(cekSenetRepository.countDurum(1L, "CIRO_EDILDI", null, null)).thenReturn(1L);
        when(cekSenetRepository.countDurum(1L, "TAHSIL_EDILDI", null, null)).thenReturn(4L);
        when(cekSenetRepository.countDurum(1L, "PROTESTO", null, null)).thenReturn(1L);
        when(cekSenetRepository.countVadesiGecen(eq(1L), any(LocalDate.class))).thenReturn(5L);
        when(cekSenetRepository.countVadesiGelen(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(6L);
        when(cekSenetRepository.toplamTutar(1L, null, null)).thenReturn(BigDecimal.valueOf(12345));

        CekSenetOzetDTO ozet = cekSenetService.ozet(1L, null, null);

        assertEquals(3, ozet.getPortfoy());
        assertEquals(2, ozet.getTeminatta());
        assertEquals(1, ozet.getCiroEdilen());
        assertEquals(4, ozet.getTahsilEdilen());
        assertEquals(1, ozet.getProtesto());
        assertEquals(5, ozet.getVadesiGecen());
        assertEquals(6, ozet.getBuAyVadesiGelen());
        assertEquals(0, BigDecimal.valueOf(12345).compareTo(ozet.getToplamTutar()));
    }

    @Test
    void takvim_bosGunleriDoldurur() {
        LocalDate bas = LocalDate.of(2026, 3, 1);
        LocalDate bit = LocalDate.of(2026, 3, 3);
        CekSenet cs = createCekSenet(1L);
        cs.setVadeTarihi(bas.plusDays(1));
        when(cekSenetRepository.findBySirketIdAndVadeTarihiBetweenOrderByVadeTarihiAsc(1L, bas, bit))
                .thenReturn(List.of(cs));

        List<CekSenetTakvimDTO> takvim = cekSenetService.takvim(1L, bas, bit);

        assertEquals(3, takvim.size());
        assertEquals(0, takvim.get(0).getAdet());
        assertEquals(1, takvim.get(1).getAdet());
        assertEquals(0, takvim.get(2).getAdet());
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(takvim.get(1).getTutar()));
        assertEquals(1, takvim.get(1).getKayitlar().size());
    }

    @Test
    void takvim_62GundenFazlaAralikReddedilir() {
        LocalDate bas = LocalDate.of(2026, 1, 1);

        var ex = assertThrows(BusinessException.class,
                () -> cekSenetService.takvim(1L, bas, bas.plusDays(62)));

        assertTrue(ex.getMessage().contains("62"));
        verifyNoInteractions(cekSenetRepository);
    }

    @Test
    void takvim_tarihZorunlu() {
        assertThrows(BusinessException.class, () -> cekSenetService.takvim(1L, null, LocalDate.now()));
        assertThrows(BusinessException.class,
                () -> cekSenetService.takvim(1L, LocalDate.now(), LocalDate.now().minusDays(1)));
    }

    // ------------------------------------------------------------------
    // Düzenleme
    // ------------------------------------------------------------------

    @Test
    void guncelle_ayniDurumdaGuncellerVeLogYazar() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        kaydetDonusu();
        CekSenetDTO dto = CekSenetDTO.builder().tur("CEK").cariHesapId(1L)
                .vadeTarihi(cs.getVadeTarihi()).tutar(BigDecimal.valueOf(12000)).durum("PORTFOY").build();

        var result = cekSenetService.guncelle(1L, dto);

        assertEquals(0, BigDecimal.valueOf(12000).compareTo(result.getTutar()));
        verify(cekSenetDurumLogRepository).save(argThat(l -> "GUNCELLE".equals(l.getIslem())));
    }

    @Test
    void guncelle_durumTahsilEdildiyeCevrilemez() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        CekSenetDTO dto = CekSenetDTO.builder().tur("CEK")
                .vadeTarihi(cs.getVadeTarihi()).tutar(cs.getTutar()).durum("TAHSIL_EDILDI").build();

        assertThrows(BusinessException.class, () -> cekSenetService.guncelle(1L, dto));
        verifyNoInteractions(hareketService);
    }

    @Test
    void guncelle_gecersizDurumGecisiReddedilir() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        CekSenetDTO dto = CekSenetDTO.builder().tur("CEK")
                .vadeTarihi(cs.getVadeTarihi()).tutar(cs.getTutar()).durum("IADE").build();

        assertThrows(BusinessException.class, () -> cekSenetService.guncelle(1L, dto));
    }

    // ------------------------------------------------------------------
    // Silme
    // ------------------------------------------------------------------

    @Test
    void sil_deletes() {
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(createCekSenet(1L)));
        cekSenetService.sil(1L);
        verify(cekSenetRepository).deleteById(1L);
    }

    @Test
    void sil_throwsWhenTahsilEdildi() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("TAHSIL_EDILDI");
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        assertThrows(BusinessException.class, () -> cekSenetService.sil(1L));
    }

    @Test
    void sil_legacyOdendiSilinemez() {
        CekSenet cs = createCekSenet(1L);
        cs.setDurum("ODENDI");
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        assertThrows(BusinessException.class, () -> cekSenetService.sil(1L));
    }
}
