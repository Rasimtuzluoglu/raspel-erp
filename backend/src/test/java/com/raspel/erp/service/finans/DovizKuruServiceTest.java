package com.raspel.erp.service.finans;

import com.raspel.erp.dto.finans.DovizKuruDTO;
import com.raspel.erp.entity.finans.DovizKuru;
import com.raspel.erp.repository.finans.DovizKuruRepository;
import com.raspel.erp.service.sistem.TcmbKurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DovizKuruServiceTest {

    @Mock
    private DovizKuruRepository dovizKuruRepository;

    @Mock
    private TcmbKurService tcmbKurService;

    @InjectMocks
    private DovizKuruService dovizKuruService;

    private DovizKuru mockKur;

    @BeforeEach
    void setUp() {
        mockKur = DovizKuru.builder()
                .id(1L)
                .dovizKodu("USD")
                .dovizAdi("ABD Doları")
                .tarih(LocalDate.now())
                .alisKuru(BigDecimal.valueOf(32.50))
                .satisKuru(BigDecimal.valueOf(32.65))
                .build();
    }

    @Test
    void testGunlukKurlariGetir_MevcutListeyiDoner() {
        when(dovizKuruRepository.findByTarihOrderByDovizKoduAsc(any(LocalDate.class)))
                .thenReturn(List.of(mockKur));

        List<DovizKuruDTO> result = dovizKuruService.gunlukKurlariGetir(LocalDate.now());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("USD", result.get(0).getDovizKodu());
    }

    @Test
    void testGunlukKurlariGetir_BosIseTcmbDenTazeler() {
        when(dovizKuruRepository.findByTarihOrderByDovizKoduAsc(any(LocalDate.class)))
                .thenReturn(Collections.emptyList(), List.of(mockKur));

        List<DovizKuruDTO> result = dovizKuruService.gunlukKurlariGetir(LocalDate.now());

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tcmbKurService, times(1)).tcmbKurlariniGuncelle();
    }

    @Test
    void testGunlukKurlariGetir_BosVeTcmbCekemezseVarsayilanlariOlusturur() {
        when(dovizKuruRepository.findByTarihOrderByDovizKoduAsc(any(LocalDate.class)))
                .thenReturn(Collections.emptyList(), Collections.emptyList());
        when(dovizKuruRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<DovizKuruDTO> result = dovizKuruService.gunlukKurlariGetir(LocalDate.now());

        assertNotNull(result);
        assertEquals(3, result.size()); // USD, EUR, GBP
        verify(dovizKuruRepository, times(1)).saveAll(any());
    }

    @Test
    void testKurEkleVeyaGuncelle_YeniKur() {
        when(dovizKuruRepository.findByDovizKoduAndTarih(eq("USD"), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(dovizKuruRepository.save(any(DovizKuru.class))).thenReturn(mockKur);

        DovizKuruDTO dto = DovizKuruDTO.builder()
                .dovizKodu("USD")
                .dovizAdi("ABD Doları")
                .alisKuru(BigDecimal.valueOf(32.50))
                .satisKuru(BigDecimal.valueOf(32.65))
                .build();

        DovizKuruDTO result = dovizKuruService.kurEkleVeyaGuncelle(dto);

        assertNotNull(result);
        assertEquals("USD", result.getDovizKodu());
        verify(dovizKuruRepository, times(1)).save(any(DovizKuru.class));
    }
}
