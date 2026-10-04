package com.raspel.erp.service.ticaret;

import com.raspel.erp.dto.ticaret.SurucuDTO;
import com.raspel.erp.dto.ticaret.TeslimatDTO;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.Teslimat;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.TeslimatDurumLogRepository;
import com.raspel.erp.repository.ticaret.TeslimatRepository;
import com.raspel.erp.service.sistem.BildirimService;
import com.raspel.erp.service.sistem.DosyaDepolamaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeslimatServiceTest {

    @Mock private TeslimatRepository teslimatRepository;
    @Mock private KullaniciRepository kullaniciRepository;
    @Mock private FaturaRepository faturaRepository;
    @Mock private DosyaDepolamaService dosyaDepolama;
    @Mock private BildirimService bildirimService;
    @Mock private TeslimatDurumLogRepository durumLogRepository;
    @Mock private com.raspel.erp.repository.ik.PersonelRepository personelRepository;
    @Mock private com.raspel.erp.repository.ticaret.SiparisRepository siparisRepository;
    @Mock private com.raspel.erp.service.sistem.PdfRaporService pdfRaporService;
    @InjectMocks private TeslimatService teslimatService;

    @Test
    void suruculer_driverOlmayanKullaniciIcinTumSuruculeriDoner() {
        Kullanici s1 = Kullanici.builder().id(1L).displayName("Ali").role("DRIVER").build();
        Kullanici s2 = Kullanici.builder().id(2L).displayName("Veli").role("DRIVER").build();
        when(kullaniciRepository.findBySirketIdAndRole(1L, "DRIVER")).thenReturn(List.of(s1, s2));
        when(personelRepository.findBySirketIdAndRolAndAktifTrue(1L, "SOFOR")).thenReturn(List.of());
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.of(Kullanici.builder().id(99L).role("USER").build()));
        when(teslimatRepository.countBySirketIdAndDriverIdAndDurumIn(eq(1L), eq(1L), anyList())).thenReturn(2L);
        when(teslimatRepository.countBySirketIdAndDriverIdAndDurumIn(eq(1L), eq(2L), anyList())).thenReturn(0L);

        List<SurucuDTO> sonuc = teslimatService.suruculer(1L, 99L);

        assertEquals(2, sonuc.size());
        assertEquals(2L, sonuc.get(0).getBekleyenTeslimatSayisi());
    }

    @Test
    void suruculer_driverKendiDisindakiSurucuyuGoremez() {
        Kullanici s1 = Kullanici.builder().id(1L).displayName("Ali").role("DRIVER").build();
        when(kullaniciRepository.findBySirketIdAndRole(1L, "DRIVER")).thenReturn(List.of(s1));
        when(personelRepository.findBySirketIdAndRolAndAktifTrue(1L, "SOFOR")).thenReturn(List.of());
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(Kullanici.builder().id(1L).role("DRIVER").build()));
        when(teslimatRepository.countBySirketIdAndDriverIdAndDurumIn(1L, 1L, List.of("BEKLEMEDE", "YOLDA"))).thenReturn(1L);

        List<SurucuDTO> sonuc = teslimatService.suruculer(1L, 1L);

        assertEquals(1, sonuc.size());
        assertEquals(1L, sonuc.get(0).getId());
    }

    @Test
    void olustur_surucuOlmayanKullaniciHataVerir() {
        Fatura f = Fatura.builder().id(10L).sirketId(1L).faturaNumarasi("F-1").build();
        when(faturaRepository.findById(10L)).thenReturn(Optional.of(f));
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(Kullanici.builder().id(5L).role("USER").build()));

        TeslimatDTO dto = TeslimatDTO.builder().faturaId(10L).driverId(5L).teslimatAdresi("Adres").build();
        assertThrows(BusinessException.class, () -> teslimatService.olustur(dto, 1L));
    }

    @Test
    void olustur_basariliKayitOlusturur() {
        Fatura f = Fatura.builder().id(10L).sirketId(1L).faturaNumarasi("F-1").build();
        Kullanici surucu = Kullanici.builder().id(5L).displayName("Ali").role("DRIVER").build();
        when(faturaRepository.findById(10L)).thenReturn(Optional.of(f));
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(surucu));
        when(teslimatRepository.saveAndFlush(any(Teslimat.class))).thenAnswer(inv -> {
            Teslimat t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TeslimatDTO dto = TeslimatDTO.builder().faturaId(10L).driverId(5L).teslimatAdresi("Adres").build();
        TeslimatDTO sonuc = teslimatService.olustur(dto, 1L);

        assertEquals(1L, sonuc.getId());
        assertEquals("F-1", sonuc.getFaturaNumarasi());
        assertEquals("BEKLEMEDE", sonuc.getDurum());
    }

    @Test
    void olustur_soforPersonelineBagliKullaniciyiKabulEder() {
        Fatura f = Fatura.builder().id(10L).sirketId(1L).faturaNumarasi("F-1").build();
        Kullanici surucu = Kullanici.builder().id(7L).sirketId(1L).displayName("Personelli Sofor").role("USER").build();
        when(faturaRepository.findById(10L)).thenReturn(Optional.of(f));
        when(kullaniciRepository.findById(7L)).thenReturn(Optional.of(surucu));
        when(personelRepository.findBySirketIdAndRolAndAktifTrue(1L, "SOFOR"))
                .thenReturn(List.of(com.raspel.erp.entity.ik.Personel.builder().id(3L).kullaniciId(7L).build()));
        when(teslimatRepository.saveAndFlush(any(Teslimat.class))).thenAnswer(inv -> {
            Teslimat t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TeslimatDTO dto = TeslimatDTO.builder().faturaId(10L).driverId(7L).teslimatAdresi("Adres").build();
        TeslimatDTO sonuc = teslimatService.olustur(dto, 1L);

        assertEquals(1L, sonuc.getId());
    }

    @Test
    void olustur_ayniFaturayaIkinciTeslimatReddedilir() {
        Fatura f = Fatura.builder().id(10L).sirketId(1L).faturaNumarasi("F-1").build();
        Kullanici surucu = Kullanici.builder().id(5L).displayName("Ali").role("DRIVER").build();
        when(faturaRepository.findById(10L)).thenReturn(Optional.of(f));
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(surucu));
        when(teslimatRepository.findBySirketIdAndFaturaId(1L, 10L))
                .thenReturn(List.of(Teslimat.builder().id(9L).build()));

        TeslimatDTO dto = TeslimatDTO.builder().faturaId(10L).driverId(5L).teslimatAdresi("Adres").build();
        assertThrows(BusinessException.class, () -> teslimatService.olustur(dto, 1L));
    }

    @Test
    void olustur_fisTeslimEdenAdiniSenkronlar() {
        Fatura f = Fatura.builder().id(10L).sirketId(1L).faturaNumarasi("F-1").build();
        Kullanici surucu = Kullanici.builder().id(5L).displayName("Ali Veli").role("DRIVER").build();
        when(faturaRepository.findById(10L)).thenReturn(Optional.of(f));
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(surucu));
        when(teslimatRepository.saveAndFlush(any(Teslimat.class))).thenAnswer(inv -> {
            Teslimat t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TeslimatDTO dto = TeslimatDTO.builder().faturaId(10L).driverId(5L).teslimatAdresi("Adres").build();
        teslimatService.olustur(dto, 1L);

        assertEquals("Ali Veli", f.getTeslimEden());
        verify(faturaRepository).save(f);
    }

    @Test
    void atanabilirFaturalar_dtoMapler() {
        Fatura f = Fatura.builder().id(10L).sirketId(1L).faturaNumarasi("F-1")
                .tarih(LocalDate.now()).genelToplam(java.math.BigDecimal.valueOf(120))
                .cariHesap(com.raspel.erp.entity.finans.CariHesap.builder().id(2L).ad("A Ltd").adres("Adres 1").build())
                .build();
        when(faturaRepository.atanabilirFaturalar(eq(1L), isNull(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(List.of(f));

        var sonuc = teslimatService.atanabilirFaturalar(1L, null, 50);

        assertEquals(1, sonuc.size());
        assertEquals("F-1", sonuc.get(0).getFaturaNumarasi());
        assertEquals("A Ltd", sonuc.get(0).getCariHesapAd());
        assertEquals("Adres 1", sonuc.get(0).getCariAdres());
    }

    @Test
    void durumGuncelle_durumLoguYazar() {
        Teslimat t = Teslimat.builder().id(1L).sirketId(1L).driverId(5L).durum("BEKLEMEDE").build();
        when(teslimatRepository.findById(1L)).thenReturn(Optional.of(t));
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.of(Kullanici.builder().id(99L).role("USER").build()));
        when(teslimatRepository.save(any(Teslimat.class))).thenReturn(t);

        teslimatService.durumGuncelle(1L, "YOLDA", 1L, 99L);

        assertEquals("YOLDA", t.getDurum());
        verify(durumLogRepository).save(argThat(l -> "BEKLEMEDE".equals(l.getOncekiDurum()) && "YOLDA".equals(l.getYeniDurum())));
    }

    // durumGuncelle hem teslimat satirini hem de denetim izi olan durum logunu yazar.
    // @Transactional olmazsa bu iki yazma ayri ayri otomatik-commit olur ve biri
    // basarisiz oldugunda tutarsiz durum birakilir. 4 argumanli overload da
    // self-invocation ile Spring proxy'sini atladigi icin asil metot islemli olmalidir.
    @Test
    void durumGuncelle_islemAnnosasyonuMevcut() throws Exception {
        java.lang.reflect.Method asil = TeslimatService.class
                .getMethod("durumGuncelle", Long.class, String.class, String.class, Long.class, Long.class);
        org.springframework.transaction.annotation.Transactional tx =
                asil.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertNotNull(tx, "durumGuncelle(id, durum, sebep, sirketId, kullaniciId) @Transactional olmali");
        assertFalse(tx.readOnly(), "durumGuncelle yazma yaptigi icin readOnly olmamali");
    }

    @Test
    void durumGuncelle_iptalSebebiZorunluVeNotaYazilir() {
        Teslimat t = Teslimat.builder().id(1L).sirketId(1L).driverId(5L).durum("YOLDA").notlar("Onceki not").build();
        when(teslimatRepository.findById(1L)).thenReturn(Optional.of(t));
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.of(Kullanici.builder().id(99L).role("USER").build()));
        when(teslimatRepository.save(any(Teslimat.class))).thenReturn(t);

        BusinessException hata = assertThrows(BusinessException.class,
                () -> teslimatService.durumGuncelle(1L, "IPTAL", "  ", 1L, 99L));
        assertTrue(hata.getMessage().contains("sebep"));

        teslimatService.durumGuncelle(1L, "IPTAL", "Müşteri reddetti", 1L, 99L);

        assertTrue(t.getNotlar().contains("İptal sebebi: Müşteri reddetti"));
        assertNull(t.getTeslimTarihi(), "IPTAL durumunda teslim tarihi temizlenmeli");
        verify(durumLogRepository, times(1)).save(any());
    }

    @Test
    void fotoYukle_urlAyarlar() throws Exception {
        Teslimat t = Teslimat.builder().id(1L).sirketId(1L).driverId(5L).durum("BEKLEMEDE").build();
        when(teslimatRepository.findById(1L)).thenReturn(Optional.of(t));
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(Kullanici.builder().id(5L).role("DRIVER").build()));
        when(dosyaDepolama.kaydetResimDogrulamali(anyString(), any())).thenReturn("foto.jpg");
        when(teslimatRepository.save(any(Teslimat.class))).thenReturn(t);

        MockMultipartFile file = new MockMultipartFile("file", "f.jpg", "image/jpeg", new byte[]{1, 2, 3});
        TeslimatDTO sonuc = teslimatService.fotoYukle(1L, file, 1L, 5L);

        assertEquals("/api/uploads/teslimat-fotolari/foto.jpg", sonuc.getTeslimatFoto());
    }

    @Test
    void gecikmisTeslimatlar_repoYonteminiCagirir() {
        when(teslimatRepository.findByDurumInAndBeklenenTeslimTarihiBeforeAndGecikmeBildirildiFalse(
                eq(List.of("BEKLEMEDE", "YOLDA")), any(LocalDate.class))).thenReturn(List.of());

        assertTrue(teslimatService.gecikmisTeslimatlar().isEmpty());
    }

    @Test
    void siparisTeslimatiUpsert_yeniTeslimatOlusturur() {
        when(teslimatRepository.findBySirketIdAndSiparisId(1L, 10L)).thenReturn(List.of());
        when(teslimatRepository.save(any(Teslimat.class))).thenAnswer(inv -> {
            Teslimat t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(Kullanici.builder().id(5L).displayName("Ali").build()));

        TeslimatDTO sonuc = teslimatService.siparisTeslimatiUpsert(10L, 5L, 1L, "A Ltd", "Adres");

        assertEquals(10L, sonuc.getSiparisId());
        assertEquals(5L, sonuc.getDriverId());
        assertEquals("BEKLEMEDE", sonuc.getDurum());
        assertEquals("Ali", sonuc.getDriverAd());
    }

    @Test
    void siparisTeslimatiUpsert_mevcutTeslimattaSoforGunceller() {
        Teslimat mevcut = Teslimat.builder().id(1L).sirketId(1L).siparisId(10L).driverId(3L).durum("BEKLEMEDE").build();
        when(teslimatRepository.findBySirketIdAndSiparisId(1L, 10L)).thenReturn(List.of(mevcut));
        when(teslimatRepository.save(any(Teslimat.class))).thenReturn(mevcut);
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(Kullanici.builder().id(5L).displayName("Veli").build()));

        TeslimatDTO sonuc = teslimatService.siparisTeslimatiUpsert(10L, 5L, 1L, "A Ltd", "Adres");

        assertEquals(5L, mevcut.getDriverId());
        assertEquals(5L, sonuc.getDriverId());
    }

    @Test
    void teslimEt_imzaZorunlu() {
        Teslimat t = Teslimat.builder().id(1L).sirketId(1L).driverId(5L).durum("YOLDA").build();
        when(teslimatRepository.findById(1L)).thenReturn(Optional.of(t));
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(Kullanici.builder().id(5L).role("DRIVER").build()));

        var istek = new TeslimatService.TeslimIstegi("Ahmet Yılmaz", null, null);
        assertThrows(BusinessException.class, () -> teslimatService.teslimEt(1L, istek, null, 1L, 5L));
    }

    @Test
    void teslimEt_teslimAlanZorunlu() {
        Teslimat t = Teslimat.builder().id(1L).sirketId(1L).driverId(5L).durum("YOLDA").build();
        when(teslimatRepository.findById(1L)).thenReturn(Optional.of(t));
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(Kullanici.builder().id(5L).role("DRIVER").build()));
        MockMultipartFile imza = new MockMultipartFile("file", "i.png", "image/png", new byte[]{1, 2, 3});

        var istek = new TeslimatService.TeslimIstegi("  ", null, null);
        assertThrows(BusinessException.class, () -> teslimatService.teslimEt(1L, istek, imza, 1L, 5L));
    }

    @Test
    void teslimEt_basariliKayitVeFaturaSenkronu() throws Exception {
        Teslimat t = Teslimat.builder().id(1L).sirketId(1L).driverId(5L).faturaId(10L).durum("YOLDA").build();
        when(teslimatRepository.findById(1L)).thenReturn(Optional.of(t));
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(Kullanici.builder().id(5L).role("DRIVER").displayName("Ali").build()));
        when(dosyaDepolama.kaydetResimDogrulamali(eq("teslimat-imzalari"), any())).thenReturn("imza.png");
        when(teslimatRepository.save(any(Teslimat.class))).thenAnswer(inv -> inv.getArgument(0));
        Fatura f = Fatura.builder().id(10L).sirketId(1L).build();
        when(faturaRepository.findById(10L)).thenReturn(Optional.of(f));
        MockMultipartFile imza = new MockMultipartFile("file", "i.png", "image/png", new byte[]{1, 2, 3});

        var istek = new TeslimatService.TeslimIstegi("Ahmet Yılmaz", "Hasarsız", "41.0, 29.0");
        TeslimatDTO sonuc = teslimatService.teslimEt(1L, istek, imza, 1L, 5L);

        assertEquals("TESLIM_EDILDI", sonuc.getDurum());
        assertEquals("Ahmet Yılmaz", sonuc.getTeslimAlanAd());
        assertEquals("/api/uploads/teslimat-imzalari/imza.png", sonuc.getTeslimImzaUrl());
        assertEquals("Ali", sonuc.getTeslimEdenAd());
        verify(durumLogRepository).save(any());
        verify(faturaRepository).save(f);
        assertEquals("TESLIM_EDILDI", f.getTeslimDurumu());
    }

    @Test
    void teslimEtSiparis_siparisiTeslimEdildiYapar() throws Exception {
        com.raspel.erp.entity.ticaret.Siparis s = com.raspel.erp.entity.ticaret.Siparis.builder()
                .id(10L).sirketId(1L).durum("YOLDA").build();
        when(siparisRepository.findById(10L)).thenReturn(Optional.of(s));
        when(teslimatRepository.findBySirketIdAndSiparisId(1L, 10L)).thenReturn(List.of());
        when(teslimatRepository.save(any(Teslimat.class))).thenAnswer(inv -> {
            Teslimat t = inv.getArgument(0);
            if (t.getId() == null) t.setId(1L);
            return t;
        });
        Teslimat kayitli = Teslimat.builder().id(1L).sirketId(1L).siparisId(10L).driverId(5L).durum("BEKLEMEDE").build();
        when(teslimatRepository.findById(1L)).thenReturn(Optional.of(kayitli));
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(Kullanici.builder().id(5L).role("DRIVER").displayName("Ali").build()));
        when(dosyaDepolama.kaydetResimDogrulamali(eq("teslimat-imzalari"), any())).thenReturn("imza.png");
        when(siparisRepository.save(any(com.raspel.erp.entity.ticaret.Siparis.class))).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile imza = new MockMultipartFile("file", "i.png", "image/png", new byte[]{1, 2, 3});

        var istek = new TeslimatService.TeslimIstegi("Ahmet", null, null);
        TeslimatDTO sonuc = teslimatService.teslimEtSiparis(10L, istek, imza, 1L, 5L);

        assertEquals("TESLIM_EDILDI", sonuc.getDurum());
        assertEquals("TESLIM_EDILDI", s.getDurum());
        verify(siparisRepository).save(s);
    }
}
