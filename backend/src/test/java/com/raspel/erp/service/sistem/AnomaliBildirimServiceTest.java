package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AnomaliDTO;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.repository.sistem.SirketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnomaliBildirimServiceTest {

    @Mock
    private AnomaliTespitEngine anomaliTespitEngine;
    @Mock
    private SirketRepository sirketRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AnomaliBildirimService anomaliBildirimService;

    @Test
    void anomaliBildir_bosListedeHicbirAksiyonYapmaz() {
        anomaliBildirimService.anomaliBildir(Sirket.builder().id(1L).build(), List.of());
        verify(emailService, never()).emailGonder(anyString(), anyString(), anyString());
    }

    @Test
    void anomaliBildir_emailAdresineGonderir() {
        ReflectionTestUtils.setField(anomaliBildirimService, "alertEmail", "admin@test.com");
        AnomaliDTO a = AnomaliDTO.builder()
                .tur("MUKERRER_FATURA").seviye("YUKSEK").baslik("Mükerrer fatura").aciklama("Aynı fatura iki kez").build();

        anomaliBildirimService.anomaliBildir(Sirket.builder().id(1L).build(), List.of(a));

        verify(emailService).emailGonder(eq("admin@test.com"), anyString(), anyString());
    }

    @Test
    void anomaliBildir_alertEmailBosIsSirketEmailKullanir() {
        ReflectionTestUtils.setField(anomaliBildirimService, "alertEmail", "");
        Sirket sirket = Sirket.builder().id(1L).email("sirket@test.com").build();
        AnomaliDTO a = AnomaliDTO.builder().tur("ANORMAL_MASRAF").seviye("ORTA").build();

        anomaliBildirimService.anomaliBildir(sirket, List.of(a));

        verify(emailService).emailGonder(eq("sirket@test.com"), anyString(), anyString());
    }

    @Test
    void anomaliBildir_aliciYoksaEmailGonderilmez() {
        ReflectionTestUtils.setField(anomaliBildirimService, "alertEmail", "");
        anomaliBildirimService.anomaliBildir(Sirket.builder().id(1L).build(), List.of(
                AnomaliDTO.builder().tur("MUKERRER_ODEME").seviye("ORTA").build()));
        verify(emailService, never()).emailGonder(anyString(), anyString(), anyString());
    }

    @Test
    void gunlukAnomaliTarama_bildirilecekTurleriFiltreler() {
        ReflectionTestUtils.setField(anomaliBildirimService, "alertEmail", "admin@test.com");
        Sirket sirket = Sirket.builder().id(1L).ad("Test").email("sirket@test.com").build();
        when(sirketRepository.findByAktifTrue()).thenReturn(List.of(sirket));

        List<AnomaliDTO> anomaliler = List.of(
                AnomaliDTO.builder().tur("MUKERRER_FATURA").seviye("YUKSEK").build(),
                AnomaliDTO.builder().tur("ANORMAL_STOK_CIKISI").seviye("ORTA").build()
        );
        when(anomaliTespitEngine.anomalileriTara(1L)).thenReturn(anomaliler);

        anomaliBildirimService.gunlukAnomaliTarama();

        verify(emailService, times(1)).emailGonder(anyString(), anyString(), anyString());
    }

    @Test
    void gunlukAnomaliTarama_hataDurumundaDevamEder() {
        Sirket sirket = Sirket.builder().id(1L).ad("Test").email("sirket@test.com").build();
        when(sirketRepository.findByAktifTrue()).thenReturn(List.of(sirket));
        when(anomaliTespitEngine.anomalileriTara(1L)).thenThrow(new RuntimeException("DB hatası"));

        anomaliBildirimService.gunlukAnomaliTarama();

        verify(emailService, never()).emailGonder(anyString(), anyString(), anyString());
    }
}
