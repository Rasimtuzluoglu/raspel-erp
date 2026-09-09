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
    @InjectMocks private TeslimatService teslimatService;

    @Test
    void suruculer_driverOlmayanKullaniciIcinTumSuruculeriDoner() {
        Kullanici s1 = Kullanici.builder().id(1L).displayName("Ali").role("DRIVER").build();
        Kullanici s2 = Kullanici.builder().id(2L).displayName("Veli").role("DRIVER").build();
        when(kullaniciRepository.findBySirketIdAndRole(1L, "DRIVER")).thenReturn(List.of(s1, s2));
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
        when(teslimatRepository.save(any(Teslimat.class))).thenAnswer(inv -> {
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
    void durumGuncelle_durumLoguYazar() {
        Teslimat t = Teslimat.builder().id(1L).sirketId(1L).driverId(5L).durum("BEKLEMEDE").build();
        when(teslimatRepository.findById(1L)).thenReturn(Optional.of(t));
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.of(Kullanici.builder().id(99L).role("USER").build()));
        when(teslimatRepository.save(any(Teslimat.class))).thenReturn(t);

        teslimatService.durumGuncelle(1L, "YOLDA", 1L, 99L);

        assertEquals("YOLDA", t.getDurum());
        verify(durumLogRepository).save(argThat(l -> "BEKLEMEDE".equals(l.getOncekiDurum()) && "YOLDA".equals(l.getYeniDurum())));
    }

    @Test
    void fotoYukle_urlAyarlar() throws Exception {
        Teslimat t = Teslimat.builder().id(1L).sirketId(1L).driverId(5L).durum("BEKLEMEDE").build();
        when(teslimatRepository.findById(1L)).thenReturn(Optional.of(t));
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(Kullanici.builder().id(5L).role("DRIVER").build()));
        when(dosyaDepolama.kaydet(anyString(), any())).thenReturn("foto.jpg");
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
}
