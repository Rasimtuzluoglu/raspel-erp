package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.SohbetOdaDTO;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.SohbetMesaj;
import com.raspel.erp.entity.sistem.SohbetOda;
import com.raspel.erp.entity.sistem.SohbetOdaUye;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.SohbetMesajRepository;
import com.raspel.erp.repository.sistem.SohbetOdaRepository;
import com.raspel.erp.repository.sistem.SohbetOdaUyeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SohbetOdaServiceTest {

    @Mock private SohbetOdaRepository odaRepository;
    @Mock private SohbetOdaUyeRepository uyeRepository;
    @Mock private SohbetMesajRepository mesajRepository;
    @Mock private KullaniciRepository kullaniciRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private DosyaDepolamaService dosyaDepolama;
    @InjectMocks private SohbetOdaService odaService;

    @Test
    void olustur_bosAdHataVerir() {
        assertThrows(BusinessException.class,
                () -> odaService.olustur(SohbetOdaDTO.builder().ad("  ").build(), 1L, 10L));
    }

    @Test
    void olustur_odayiOlustururVeKurucuyuUyeYapar() {
        when(odaRepository.save(any(SohbetOda.class))).thenAnswer(inv -> {
            SohbetOda o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });
        SohbetOdaDTO sonuc = odaService.olustur(SohbetOdaDTO.builder().ad("Satış Ekibi").build(), 1L, 10L);

        assertEquals(1L, sonuc.getId());
        assertTrue(sonuc.isUyeMi());
        verify(uyeRepository).save(argThat(u -> u.getOdaId().equals(1L) && u.getKullaniciId().equals(10L)));
    }

    @Test
    void mesajGonder_uyeDegilseHataVerir() {
        when(odaRepository.findById(1L)).thenReturn(Optional.of(SohbetOda.builder().id(1L).sirketId(1L).build()));
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.of(Kullanici.builder().id(99L).role("USER").build()));
        when(uyeRepository.existsByOdaIdAndKullaniciId(1L, 99L)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> odaService.mesajGonder(1L, com.raspel.erp.dto.sistem.SohbetMesajDTO.builder().mesaj("merhaba").build(),
                        1L, 99L, "Ali"));
    }

    @Test
    void mesajGonder_uyeIseKaydeder() {
        when(odaRepository.findById(1L)).thenReturn(Optional.of(SohbetOda.builder().id(1L).sirketId(1L).build()));
        when(uyeRepository.existsByOdaIdAndKullaniciId(1L, 99L)).thenReturn(true);
        when(mesajRepository.save(any(SohbetMesaj.class))).thenAnswer(inv -> {
            SohbetMesaj m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        var sonuc = odaService.mesajGonder(1L,
                com.raspel.erp.dto.sistem.SohbetMesajDTO.builder().mesaj("merhaba").build(),
                1L, 99L, "Ali");

        assertEquals("merhaba", sonuc.getMesaj());
        assertEquals(1L, sonuc.getOdaId());
    }

    @Test
    void okunduIsaretle_sonOkumayiGunceller() {
        SohbetOdaUye uye = SohbetOdaUye.builder().odaId(1L).kullaniciId(99L).build();
        when(uyeRepository.findByOdaIdAndKullaniciId(1L, 99L)).thenReturn(Optional.of(uye));

        odaService.okunduIsaretle(1L, 99L);

        assertNotNull(uye.getSonOkuma());
        verify(uyeRepository).save(uye);
    }

    @Test
    void dosyaYukle_uyeDegilseHataVerir() {
        when(odaRepository.findById(1L)).thenReturn(Optional.of(SohbetOda.builder().id(1L).sirketId(1L).build()));
        when(uyeRepository.existsByOdaIdAndKullaniciId(1L, 99L)).thenReturn(false);
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.of(Kullanici.builder().id(99L).role("USER").build()));

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        assertThrows(BusinessException.class, () -> odaService.dosyaYukle(1L, file, 1L, 99L));
    }

    @Test
    void dosyaYukle_urlDondurur() throws Exception {
        when(odaRepository.findById(1L)).thenReturn(Optional.of(SohbetOda.builder().id(1L).sirketId(1L).build()));
        when(uyeRepository.existsByOdaIdAndKullaniciId(1L, 99L)).thenReturn(true);
        when(dosyaDepolama.kaydetResimDogrulamali(anyString(), any())).thenReturn("a.png");

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        String url = odaService.dosyaYukle(1L, file, 1L, 99L);

        assertEquals("/api/uploads/sohbet/a.png", url);
    }

    /**
     * C7: sohbet eki artık düz `sohbet/` klasörüne değil `sohbet/s{sirketId}`
     * altına yazılır. Düz klasördeki bir dosyanın URL'i UUID'sini bilen herhangi
     * bir kullanıcı tarafından diğer şirket için de okunabiliyordu.
     */
    @Test
    void dosyaYukle_tenantKlasoruKullanir() throws Exception {
        when(odaRepository.findById(1L)).thenReturn(Optional.of(SohbetOda.builder().id(1L).sirketId(4L).build()));
        when(uyeRepository.existsByOdaIdAndKullaniciId(1L, 99L)).thenReturn(true);
        when(dosyaDepolama.kaydetResimDogrulamali(anyString(), any())).thenReturn("a.png");

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        odaService.dosyaYukle(1L, file, 4L, 99L);

        verify(dosyaDepolama).kaydetResimDogrulamali(eq("sohbet/s4"), any());
    }

    /** C7: iki farklı şirket aynı adı taşısa bile ayrı klasörlere yazılır. */
    @Test
    void dosyaYukle_farkliSirketlerAyrıKlasoreYazar() throws Exception {
        when(odaRepository.findById(1L)).thenReturn(Optional.of(SohbetOda.builder().id(1L).sirketId(1L).build()));
        when(odaRepository.findById(2L)).thenReturn(Optional.of(SohbetOda.builder().id(2L).sirketId(2L).build()));
        when(uyeRepository.existsByOdaIdAndKullaniciId(anyLong(), anyLong())).thenReturn(true);
        when(dosyaDepolama.kaydetResimDogrulamali(anyString(), any())).thenReturn("a.png");

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        odaService.dosyaYukle(1L, file, 1L, 99L);
        odaService.dosyaYukle(2L, file, 2L, 99L);

        verify(dosyaDepolama).kaydetResimDogrulamali(eq("sohbet/s1"), any());
        verify(dosyaDepolama).kaydetResimDogrulamali(eq("sohbet/s2"), any());
    }

    /** C7: sohbet dışı dosya türleri (svg/html/exe) reddedilir. */
    @Test
    void dosyaYukle_tehlikeliUzantiReddedilir() throws Exception {
        when(odaRepository.findById(1L)).thenReturn(Optional.of(SohbetOda.builder().id(1L).sirketId(1L).build()));
        when(uyeRepository.existsByOdaIdAndKullaniciId(1L, 99L)).thenReturn(true);

        MockMultipartFile svg = new MockMultipartFile("file", "x.svg", "image/svg+xml", "<svg/>".getBytes());
        assertThrows(BusinessException.class, () -> odaService.dosyaYukle(1L, svg, 1L, 99L));
        verify(dosyaDepolama, never()).kaydet(anyString(), any());
    }
}
