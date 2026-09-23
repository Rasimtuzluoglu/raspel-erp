package com.raspel.erp.service;

import com.raspel.erp.dto.ticaret.FaturaDTO;
import com.raspel.erp.dto.ticaret.FaturaKalemDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.service.sistem.BildirimService;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.service.sistem.EmailService;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.FaturaKalem;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.service.ticaret.FaturaService;
import com.raspel.erp.service.sistem.PdfRaporService;
import com.raspel.erp.service.sistem.SeriNoServisi;
import com.raspel.erp.repository.sistem.SirketRepository;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;

@ExtendWith(MockitoExtension.class)
class FaturaServiceTest {

    @Mock private FaturaRepository faturaRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private CariHesapService cariHesapService;
    @Mock private com.raspel.erp.repository.sube.DepoRepository depoRepository;
    @Mock private com.raspel.erp.service.sube.DepoStokService depoStokService;
    @Mock private com.raspel.erp.service.sistem.TcmbKurService tcmbKurService;
    @Mock private StokRepository stokRepository;
    @Mock private StokHareketRepository stokHareketRepository;
    @Mock private SeriNoServisi seriNoServisi;
    @Mock private BildirimService bildirimService;
    @Mock private EmailService emailService;
    @Mock private PdfRaporService pdfRaporService;
    @Mock private com.raspel.erp.repository.sistem.SirketRepository sirketRepository;
    @Mock private TenantChecker tenantChecker;
    @Mock private CacheYardimci cacheYardimci;
    @Mock private com.raspel.erp.service.envanter.StokSeriService stokSeriService;
    @Mock private com.raspel.erp.service.ticaret.FaturaGecmisService faturaGecmisService;
    @Mock private com.raspel.erp.service.envanter.MaliyetService maliyetService;
    @Mock private com.raspel.erp.repository.ticaret.FaturaKalemRepository faturaKalemRepository;
    @Mock private com.raspel.erp.service.sistem.DonemService donemService;
    @Mock private com.raspel.erp.service.ticaret.IskontoMotoruService iskontoMotoruService;
    @Mock private com.raspel.erp.repository.muhasebe.IrsaliyeRepository irsaliyeRepository;
    @Mock private com.raspel.erp.repository.finans.HareketRepository hareketRepository;
    @Mock private com.raspel.erp.repository.finans.KasaRepository kasaRepository;
    @Mock private com.raspel.erp.repository.finans.KasaHareketRepository kasaHareketRepository;
    @Mock private com.raspel.erp.repository.finans.BankaRepository bankaRepository;
    @Mock private com.raspel.erp.repository.finans.BankaHareketiRepository bankaHareketiRepository;
    @Mock private com.raspel.erp.service.finans.TaksitService taksitService;
    @Mock private com.raspel.erp.repository.ticaret.IadeRepository iadeRepository;
    @InjectMocks private FaturaService faturaService;

    private CariHesap createCariHesap() {
        CariHesap c = new CariHesap();
        c.setId(1L);
        c.setAd("Test Cari");
        c.setBakiye(BigDecimal.ZERO);
        c.setOlusturmaTarihi(LocalDateTime.now());
        c.setGuncellemeTarihi(LocalDateTime.now());
        return c;
    }

    private Stok createStok() {
        Stok s = new Stok();
        s.setId(1L);
        s.setAd("Test Stok");
        s.setStokKodu("STK001");
        s.setMiktar(BigDecimal.valueOf(100));
        s.setFiyat(BigDecimal.valueOf(50));
        s.setOlusturmaTarihi(LocalDateTime.now());
        return s;
    }

    private Fatura createFatura(Long id) {
        CariHesap c = createCariHesap();
        Fatura f = new Fatura();
        f.setId(id);
        f.setFaturaNumarasi("FTR-202607-0001");
        f.setTarih(LocalDate.now());
        f.setTur(Fatura.FaturaTur.SATIS);
        f.setDurum(Fatura.FaturaDurum.TASLAK);
        f.setCariHesap(c);
        f.setAraToplam(BigDecimal.valueOf(100));
        f.setKdv(BigDecimal.valueOf(20));
        f.setGenelToplam(BigDecimal.valueOf(120));
        f.setOdenenTutar(BigDecimal.ZERO);
        f.setKalanTutar(BigDecimal.valueOf(120));
        f.setOlusturmaTarihi(LocalDateTime.now());
        f.setKalemler(new ArrayList<>());
        return f;
    }

    @Test
    void tumFaturalariGetir_returnsAll() {
        when(faturaRepository.findBySirketIdOrderByTarihDesc(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(createFatura(1L))));
        var result = faturaService.tumFaturalariGetir(1L, Pageable.unpaged());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void faturaGetir_returnsFatura() {
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(createFatura(1L)));
        var result = faturaService.faturaGetir(1L);
        assertNotNull(result);
        assertEquals("FTR-202607-0001", result.getFaturaNumarasi());
    }

    @Test
    void faturaGetir_throwsWhenNotFound() {
        when(faturaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> faturaService.faturaGetir(99L));
    }

    @Test
    void faturaOlustur_creates() {
        CariHesap cari = createCariHesap();
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));
        FaturaKalemDTO kalem = FaturaKalemDTO.builder().aciklama("Kalem 1").adet(java.math.BigDecimal.valueOf(2))
                .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20)).build();
        FaturaDTO dto = FaturaDTO.builder().tur("SATIS").tarih(LocalDate.now())
                .cariHesapId(1L).kalemler(List.of(kalem)).build();
        Fatura saved = createFatura(1L);
        saved.setDurum(Fatura.FaturaDurum.KESILDI);
        when(faturaRepository.save(any(Fatura.class))).thenReturn(saved);
        var result = faturaService.faturaOlustur(dto, 1L, null, null);
        assertNotNull(result);
    }

    @Test
    void faturaOlustur_baskaSirketStoguReddedilir() {
        CariHesap cari = createCariHesap();
        cari.setSirketId(1L);
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));
        Stok yabanciStok = createStok();
        yabanciStok.setSirketId(2L);
        when(stokRepository.findAllById(anyList())).thenReturn(List.of(yabanciStok));
        FaturaKalemDTO kalem = FaturaKalemDTO.builder().aciklama("Kalem 1")
                .adet(BigDecimal.valueOf(2)).birimFiyat(BigDecimal.valueOf(100))
                .kdvOrani(BigDecimal.valueOf(20)).stokId(1L).build();
        FaturaDTO dto = FaturaDTO.builder().tur("SATIS").tarih(LocalDate.now())
                .cariHesapId(1L).kalemler(List.of(kalem)).build();

        // Çapraz-tenant yazma engeli: başka şirketin stoğu ile fatura oluşturulamaz.
        assertThrows(com.raspel.erp.exception.ResourceNotFoundException.class,
                () -> faturaService.faturaOlustur(dto, 1L, null, null));
        verify(faturaRepository, never()).save(any(Fatura.class));
    }

    @Test
    void faturaOlustur_emailBasarsaDurumuYanitaEkler() {
        CariHesap cari = createCariHesap();
        cari.setEmail("cari@example.com");
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));
        FaturaKalemDTO kalem = FaturaKalemDTO.builder().aciklama("Kalem 1").adet(java.math.BigDecimal.valueOf(2))
                .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20)).build();
        FaturaDTO dto = FaturaDTO.builder().tur("SATIS").tarih(LocalDate.now())
                .cariHesapId(1L).kalemler(List.of(kalem)).build();
        Fatura saved = createFatura(1L);
        when(faturaRepository.save(any(Fatura.class))).thenReturn(saved);
        when(emailService.faturaBildirimiGonder(eq("cari@example.com"), any(), any())).thenReturn(true);

        var result = faturaService.faturaOlustur(dto, 1L, null, null);

        assertEquals("GONDERILDI", result.getEmailGonderimDurumu());
        verify(emailService).faturaBildirimiGonder(eq("cari@example.com"), any(), any());
    }

    @Test
    void faturaOlustur_emailBasarisizsaDurumuYanitaEklerVeFaturaYineOlusur() {
        CariHesap cari = createCariHesap();
        cari.setEmail("cari@example.com");
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));
        FaturaKalemDTO kalem = FaturaKalemDTO.builder().aciklama("Kalem 1").adet(java.math.BigDecimal.valueOf(2))
                .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20)).build();
        FaturaDTO dto = FaturaDTO.builder().tur("SATIS").tarih(LocalDate.now())
                .cariHesapId(1L).kalemler(List.of(kalem)).build();
        Fatura saved = createFatura(1L);
        when(faturaRepository.save(any(Fatura.class))).thenReturn(saved);
        when(emailService.faturaBildirimiGonder(eq("cari@example.com"), any(), any())).thenReturn(false);

        var result = faturaService.faturaOlustur(dto, 1L, null, null);

        assertNotNull(result);
        assertEquals("GONDERILEMEDI", result.getEmailGonderimDurumu());
    }

    @Test
    void faturaOlustur_emailYoksaDurumNullKalir() {
        CariHesap cari = createCariHesap();
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));
        FaturaKalemDTO kalem = FaturaKalemDTO.builder().aciklama("Kalem 1").adet(java.math.BigDecimal.valueOf(2))
                .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20)).build();
        FaturaDTO dto = FaturaDTO.builder().tur("SATIS").tarih(LocalDate.now())
                .cariHesapId(1L).kalemler(List.of(kalem)).build();
        Fatura saved = createFatura(1L);
        when(faturaRepository.save(any(Fatura.class))).thenReturn(saved);

        var result = faturaService.faturaOlustur(dto, 1L, null, null);

        assertNull(result.getEmailGonderimDurumu());
        verify(emailService, never()).faturaBildirimiGonder(anyString(), anyString(), anyString());
    }

    @Test
    void faturaOlustur_throwsWhenInvalidTur() {
        FaturaDTO dto = FaturaDTO.builder().tur("INVALID").tarih(LocalDate.now()).kalemler(List.of()).build();
        assertThrows(RuntimeException.class, () -> faturaService.faturaOlustur(dto, 1L, null, null));
    }

    @Test
    void faturaOlustur_irsaliyeKesilmisseStokTekrarDusulmez() {
        CariHesap cari = createCariHesap();
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));
        com.raspel.erp.entity.muhasebe.Irsaliye irsaliye = new com.raspel.erp.entity.muhasebe.Irsaliye();
        irsaliye.setId(7L);
        irsaliye.setDurum("KESILDI");
        irsaliye.setSirketId(1L);
        when(irsaliyeRepository.findById(7L)).thenReturn(Optional.of(irsaliye));
        FaturaKalemDTO kalem = FaturaKalemDTO.builder().aciklama("K").adet(java.math.BigDecimal.valueOf(2))
                .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20)).stokId(1L).build();
        FaturaDTO dto = FaturaDTO.builder().tur("SATIS").durum("KESILDI").tarih(LocalDate.now())
                .cariHesapId(1L).irsaliyeId(7L).kalemler(List.of(kalem)).build();
        Fatura saved = createFatura(1L);
        when(faturaRepository.save(any(Fatura.class))).thenReturn(saved);

        faturaService.faturaOlustur(dto, 1L, null, null);

        // İrsaliye zaten stok işlediği için faturada stok hareketi oluşmamalı.
        verify(stokHareketRepository, never()).saveAll(any());
        verify(stokRepository, never()).save(any());
    }

    @Test
    void faturaDurumGuncelle_updatesToKesildi() {
        Fatura fatura = createFatura(1L);
        Stok stok = createStok();
        FaturaKalem kalem = FaturaKalem.builder().id(1L).fatura(fatura).aciklama("K").adet(java.math.BigDecimal.valueOf(2))
                .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20))
                .tutar(BigDecimal.valueOf(240)).stokId(1L).build();
        fatura.getKalemler().add(kalem);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
        when(faturaRepository.save(any(Fatura.class))).thenReturn(fatura);
        var result = faturaService.faturaDurumGuncelle(1L, "KESILDI");
        assertEquals("KESILDI", result.getDurum());
    }

    @Test
    void faturaDurumGuncelle_alis_increasesStock() {
        Fatura fatura = createFatura(1L);
        fatura.setTur(Fatura.FaturaTur.ALIS);
        Stok stok = createStok();
        FaturaKalem kalem = FaturaKalem.builder().id(1L).fatura(fatura).aciklama("K").adet(java.math.BigDecimal.valueOf(2))
                .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20))
                .tutar(BigDecimal.valueOf(240)).stokId(1L).build();
        fatura.getKalemler().add(kalem);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
        when(faturaRepository.save(any(Fatura.class))).thenReturn(fatura);
        var result = faturaService.faturaDurumGuncelle(1L, "KESILDI");
        assertEquals("KESILDI", result.getDurum());
        assertEquals(0, stok.getMiktar().compareTo(BigDecimal.valueOf(102)));
        assertEquals(1L, stok.getTedarikciId());
        assertEquals(0, stok.getFiyat().compareTo(new BigDecimal("50.98")));
        assertEquals(0, stok.getTedarikciFiyat().compareTo(BigDecimal.valueOf(100)));
        ArgumentCaptor<List<com.raspel.erp.entity.envanter.StokHareket>> captor = ArgumentCaptor.forClass(List.class);
        verify(stokHareketRepository).saveAll(captor.capture());
        assertEquals("GIRIS", captor.getValue().get(0).getTur());
        // Alış faturası tedarikçiyi alacaklandırır -> bakiye pozitif (alacak)
        verify(cariHesapService).bakiyeGuncelle(1L, new BigDecimal("120"));
    }

    @Test
    void faturaDurumGuncelle_satis_updatesCariBakiyeNegative() {
        Fatura fatura = createFatura(1L);
        Stok stok = createStok();
        FaturaKalem kalem = FaturaKalem.builder().id(1L).fatura(fatura).aciklama("K").adet(java.math.BigDecimal.valueOf(2))
                .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20))
                .tutar(BigDecimal.valueOf(240)).stokId(1L).build();
        fatura.getKalemler().add(kalem);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
        when(faturaRepository.save(any(Fatura.class))).thenReturn(fatura);
        faturaService.faturaDurumGuncelle(1L, "KESILDI");
        // Satış faturası müşteriyi borçlandırır -> bakiye negatif (borçlu)
        verify(cariHesapService).bakiyeGuncelle(1L, new BigDecimal("-120"));
    }

    @Test
    void faturaDurumGuncelle_kesildidenTaslakGeriAlinir() {
        Fatura fatura = createFatura(1L);
        fatura.setDurum(Fatura.FaturaDurum.KESILDI);
        Stok stok = createStok();
        stok.setMiktar(BigDecimal.valueOf(98)); // satis kesilince dusulmus varsayimi
        FaturaKalem kalem = FaturaKalem.builder().id(1L).fatura(fatura).aciklama("K").adet(java.math.BigDecimal.valueOf(2))
                .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20))
                .tutar(BigDecimal.valueOf(240)).stokId(1L).build();
        fatura.getKalemler().add(kalem);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
        when(faturaRepository.save(any(Fatura.class))).thenReturn(fatura);

        var result = faturaService.faturaDurumGuncelle(1L, "TASLAK");

        assertEquals("TASLAK", result.getDurum());
        // Stok geri eklendi (98 + 2)
        assertEquals(0, stok.getMiktar().compareTo(BigDecimal.valueOf(100)));
        // Cari borcu geri alindi (ters kayit +120)
        verify(cariHesapService).bakiyeGuncelle(1L, new BigDecimal("120"));
    }

    @Test
    void faturaDurumGuncelle_odenenFaturaIptal_kasaTersKayit() {
        Fatura fatura = createFatura(1L);
        fatura.setId(1L);
        fatura.setDurum(Fatura.FaturaDurum.KESILDI);
        fatura.setKasaId(1L);
        fatura.setOdenenTutar(BigDecimal.valueOf(120));
        fatura.setKalanTutar(BigDecimal.ZERO);

        com.raspel.erp.entity.finans.Kasa kasa = new com.raspel.erp.entity.finans.Kasa();
        kasa.setId(1L);
        kasa.setBakiye(BigDecimal.valueOf(500));
        com.raspel.erp.entity.finans.KasaHareket kh = com.raspel.erp.entity.finans.KasaHareket.builder()
                .id(9L).kasa(kasa).tur("GELIR").tutar(BigDecimal.valueOf(120)).faturaId(1L).build();

        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        when(hareketRepository.countByFaturaId(1L)).thenReturn(0L);
        when(kasaHareketRepository.findByFaturaId(1L)).thenReturn(java.util.List.of(kh));
        when(kasaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(kasa));
        when(faturaRepository.save(any(Fatura.class))).thenReturn(fatura);

        faturaService.faturaDurumGuncelle(1L, "IPTAL");

        // Peşin tahsilat kasaya girmişti; iptalde kasadan düşülür (500 - 120 = 380).
        assertEquals(0, kasa.getBakiye().compareTo(BigDecimal.valueOf(380)));
        verify(kasaHareketRepository).save(any(com.raspel.erp.entity.finans.KasaHareket.class));
    }

    @Test
    void faturaDurumGuncelle_bagliHareketVarsaIptalReddedilir() {
        Fatura fatura = createFatura(1L);
        fatura.setDurum(Fatura.FaturaDurum.KESILDI);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        when(hareketRepository.countByFaturaId(1L)).thenReturn(2L);

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> faturaService.faturaDurumGuncelle(1L, "IPTAL"));
        verify(faturaRepository, never()).save(any(Fatura.class));
    }

    @Test
    void faturaOlustur_alis_withDepo_updatesDepoStok() {
        CariHesap cari = createCariHesap();
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));
        Stok stok = createStok();
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
        when(depoStokService.coz(any(), any())).thenReturn(5L);

        FaturaKalemDTO kalem = FaturaKalemDTO.builder().aciklama("Kalem 1").adet(java.math.BigDecimal.valueOf(2))
                .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20)).stokId(1L).build();
        FaturaDTO dto = FaturaDTO.builder().tur("ALIS").durum("KESILDI").tarih(LocalDate.now())
                .cariHesapId(1L).depoId(5L).kalemler(List.of(kalem)).build();
        Fatura saved = createFatura(1L);
        saved.setTur(Fatura.FaturaTur.ALIS);
        saved.setDurum(Fatura.FaturaDurum.KESILDI);
        saved.setDepoId(5L);
        when(faturaRepository.save(any(Fatura.class))).thenReturn(saved);

        faturaService.faturaOlustur(dto, 1L, null, null);

        verify(depoStokService).guncelle(any(), any(), any());
    }

    @Test
    void faturaDurumGuncelle_throwsWhenIptalFatura() {
        Fatura fatura = createFatura(1L);
        fatura.setDurum(Fatura.FaturaDurum.IPTAL);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        assertThrows(RuntimeException.class, () -> faturaService.faturaDurumGuncelle(1L, "KESILDI"));
    }

    @Test
    void faturaDurumGuncelle_throwsWhenInvalidDurum() {
        Fatura fatura = createFatura(1L);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        assertThrows(RuntimeException.class, () -> faturaService.faturaDurumGuncelle(1L, "INVALID"));
    }

    @Test
    void faturaGuncelle_updatesDraft() {
        Fatura fatura = createFatura(1L);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        CariHesap cari = createCariHesap();
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));
        FaturaKalemDTO kalem = FaturaKalemDTO.builder().aciklama("Yeni Kalem").adet(java.math.BigDecimal.valueOf(1))
                .birimFiyat(BigDecimal.valueOf(200)).kdvOrani(BigDecimal.valueOf(20)).build();
        FaturaDTO dto = FaturaDTO.builder().tur("SATIS").tarih(LocalDate.now())
                .cariHesapId(1L).kalemler(List.of(kalem)).build();
        when(faturaRepository.save(any(Fatura.class))).thenReturn(fatura);
        var result = faturaService.faturaGuncelle(1L, dto);
        assertNotNull(result);
    }

    @Test
    void faturaGuncelle_throwsWhenIptal() {
        Fatura fatura = createFatura(1L);
        fatura.setDurum(Fatura.FaturaDurum.IPTAL);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        assertThrows(RuntimeException.class, () -> faturaService.faturaGuncelle(1L, new FaturaDTO()));
    }

    @Test
    void faturaGuncelle_kesilmisRevize_stokFarkiniIsler() {
        Fatura fatura = createFatura(1L);
        fatura.setDurum(Fatura.FaturaDurum.KESILDI);
        FaturaKalem eskiKalem = FaturaKalem.builder().id(1L).fatura(fatura).aciklama("K").adet(java.math.BigDecimal.valueOf(2))
                .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20))
                .tutar(BigDecimal.valueOf(240)).stokId(1L).build();
        fatura.getKalemler().add(eskiKalem);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(createCariHesap()));
        Stok stok = createStok();
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
        when(faturaRepository.save(any(Fatura.class))).thenReturn(fatura);

        FaturaKalemDTO kalem = FaturaKalemDTO.builder().aciklama("K").adet(java.math.BigDecimal.valueOf(5))
                .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20)).stokId(1L).build();
        FaturaDTO dto = FaturaDTO.builder().tur("SATIS").tarih(LocalDate.now())
                .cariHesapId(1L).kalemler(List.of(kalem)).build();

        faturaService.faturaGuncelle(1L, dto);

        assertEquals(0, stok.getMiktar().compareTo(BigDecimal.valueOf(97)));
    }

    @Test
    void faturaGuncelle_kesilmisOdemeli_throws() {
        Fatura fatura = createFatura(1L);
        fatura.setDurum(Fatura.FaturaDurum.KESILDI);
        fatura.setOdenenTutar(BigDecimal.valueOf(50));
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        FaturaDTO dto = FaturaDTO.builder().tur("SATIS").tarih(LocalDate.now())
                .cariHesapId(1L).kalemler(List.of()).build();
        assertThrows(RuntimeException.class, () -> faturaService.faturaGuncelle(1L, dto));
    }

    @Test
    void faturaSil_deletesDraft() {
        Fatura fatura = createFatura(1L);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        faturaService.faturaSil(1L);
        verify(faturaRepository).deleteById(1L);
    }

    @Test
    void faturaSil_throwsWhenKesildi() {
        Fatura fatura = createFatura(1L);
        fatura.setDurum(Fatura.FaturaDurum.KESILDI);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        assertThrows(RuntimeException.class, () -> faturaService.faturaSil(1L));
    }

    @Test
    void faturaSil_throwsWhenNotFound() {
        when(faturaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> faturaService.faturaSil(99L));
    }

    @Test
    void cariFaturalari_listeler() {
        Fatura f = createFatura(1L);
        when(faturaRepository.findByCariHesapIdAndSirketIdOrderByTarihDesc(eq(1L), eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(f)));

        var result = faturaService.cariFaturalari(1L, 1L, Pageable.unpaged());

        assertEquals(1, result.getContent().size());
    }

    @Test
    void faturaYenidenHesapla_dryRunKaydetmezVeYeniToplamlariDoner() {
        Fatura fatura = createFatura(1L);
        fatura.setId(1L);
        FaturaKalem kalem = FaturaKalem.builder().id(1L).fatura(fatura).aciklama("K")
                .adet(BigDecimal.valueOf(2)).birimFiyat(BigDecimal.valueOf(120))
                .kdvOrani(BigDecimal.valueOf(20)).tutar(BigDecimal.valueOf(240)).stokId(1L).build();
        fatura.getKalemler().add(kalem);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));

        var dto = faturaService.faturaYenidenHesapla(1L, false);

        // 120 (KDV dahil) x2 = 240; net 200, kdv 40
        assertEquals(0, dto.getGenelToplam().compareTo(new BigDecimal("240")));
        assertEquals(0, dto.getAraToplam().compareTo(new BigDecimal("200")));
        assertEquals(0, dto.getKdv().compareTo(new BigDecimal("40")));
        verify(faturaRepository, never()).save(any(Fatura.class));
    }

    @Test
    void faturaYenidenHesapla_kaydetFaturayiGunceller() {
        Fatura fatura = createFatura(1L);
        fatura.setId(1L);
        FaturaKalem kalem = FaturaKalem.builder().id(1L).fatura(fatura).aciklama("K")
                .adet(BigDecimal.valueOf(2)).birimFiyat(BigDecimal.valueOf(120))
                .kdvOrani(BigDecimal.valueOf(20)).tutar(BigDecimal.valueOf(240)).stokId(1L).build();
        fatura.getKalemler().add(kalem);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        when(faturaRepository.save(any(Fatura.class))).thenReturn(fatura);

        faturaService.faturaYenidenHesapla(1L, true);

        assertEquals(0, fatura.getGenelToplam().compareTo(new BigDecimal("240")));
        assertEquals(0, fatura.getAraToplam().compareTo(new BigDecimal("200")));
        assertEquals(0, kalem.getTutar().compareTo(new BigDecimal("240")));
        verify(faturaRepository).save(fatura);
    }

    @Test
    void faturaOlustur_taksit_satisindaPlanOlusturulur() {
        CariHesap cari = createCariHesap();
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));
        FaturaKalemDTO kalem = FaturaKalemDTO.builder().aciklama("K").adet(BigDecimal.ONE)
                .birimFiyat(BigDecimal.valueOf(1200)).kdvOrani(BigDecimal.valueOf(20)).build();
        FaturaDTO dto = FaturaDTO.builder().tur("SATIS").durum("KESILDI").tarih(LocalDate.now())
                .cariHesapId(1L).odemeYontemi("TAKSIT").taksitKurum("Banka").taksitSayisi(3)
                .kalemler(List.of(kalem)).build();
        Fatura saved = createFatura(1L);
        saved.setId(1L);
        saved.setOdemeYontemi("TAKSIT");
        when(faturaRepository.save(any(Fatura.class))).thenReturn(saved);

        faturaService.faturaOlustur(dto, 1L, null, null);

        verify(taksitService).planOlustur(any(com.raspel.erp.dto.finans.TaksitPlanDTO.class), eq(1L));
    }

    @Test
    void faturaNumarasiIleGetir_bulur() {
        Fatura fatura = createFatura(1L);
        fatura.setFaturaNumarasi("FTR-1-2026-000001");
        when(faturaRepository.findFirstBySirketIdAndFaturaNumarasiIgnoreCase(1L, "FTR-1-2026-000001"))
                .thenReturn(Optional.of(fatura));

        var dto = faturaService.faturaNumarasiIleGetir("FTR-1-2026-000001", 1L);

        assertEquals("FTR-1-2026-000001", dto.getFaturaNumarasi());
    }

    @Test
    void faturaNumarasiIleGetir_bulunamazsaHata() {
        when(faturaRepository.findFirstBySirketIdAndFaturaNumarasiIgnoreCase(1L, "YOK"))
                .thenReturn(Optional.empty());

        assertThrows(com.raspel.erp.exception.ResourceNotFoundException.class,
                () -> faturaService.faturaNumarasiIleGetir("YOK", 1L));
    }

    @Test
    void faturaParaIzi_kasaBankaVeIadeBirlestirir() {
        Fatura fatura = createFatura(1L);
        fatura.setKasaId(10L);
        fatura.setBankaId(20L);
        fatura.setIrsaliyeId(30L);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));

        com.raspel.erp.entity.finans.Kasa kasa =
                com.raspel.erp.entity.finans.Kasa.builder().id(10L).ad("Merkez Kasa").build();
        com.raspel.erp.entity.finans.KasaHareket kh =
                com.raspel.erp.entity.finans.KasaHareket.builder()
                        .id(100L).kasa(kasa).tur("GIRIS").tutar(BigDecimal.valueOf(500))
                        .hareketTarihi(LocalDate.of(2026, 1, 1)).aciklama("Tahsilat")
                        .kaynakTip("FATURA").build();
        when(kasaHareketRepository.findByFaturaId(1L)).thenReturn(List.of(kh));

        com.raspel.erp.entity.finans.BankaHareketi bh =
                com.raspel.erp.entity.finans.BankaHareketi.builder()
                        .id(200L).bankaId(20L).tarih(LocalDate.of(2026, 1, 2))
                        .borc(BigDecimal.ZERO).alacak(BigDecimal.valueOf(300)).eslestirildi(false)
                        .aciklama("Havale").kaynakTip("FATURA").build();
        when(bankaHareketiRepository.findByKaynakFaturaId(1L)).thenReturn(List.of(bh));
        when(bankaRepository.findById(20L)).thenReturn(Optional.of(
                com.raspel.erp.entity.finans.Banka.builder().id(20L).ad("Ziraat").build()));
        when(kasaRepository.findById(10L)).thenReturn(Optional.of(kasa));

        com.raspel.erp.entity.ticaret.Iade iade =
                com.raspel.erp.entity.ticaret.Iade.builder()
                        .id(300L).faturaId(1L).tur("SATIS").tarih(LocalDate.of(2026, 1, 3))
                        .tutar(BigDecimal.valueOf(50)).durum("ONAYLANDI").aciklama("Iade").build();
        when(iadeRepository.findByFaturaIdInAndSirketId(anyList(), any())).thenReturn(List.of(iade));

        var sonuc = faturaService.faturaParaIzi(1L);

        assertEquals(1, sonuc.getKasaHareketleri().size());
        assertEquals("Merkez Kasa", sonuc.getKasaHareketleri().get(0).getHesapAd());
        assertEquals(1, sonuc.getBankaHareketleri().size());
        assertEquals("GIRIS", sonuc.getBankaHareketleri().get(0).getTur());
        assertEquals("Ziraat", sonuc.getBankaHareketleri().get(0).getHesapAd());
        assertEquals(1, sonuc.getIadeler().size());
        assertEquals(30L, sonuc.getIrsaliyeId());
    }

    @Test
    void faturaParaIzi_bulunamazsaHata() {
        when(faturaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(com.raspel.erp.exception.ResourceNotFoundException.class,
                () -> faturaService.faturaParaIzi(99L));
    }
}