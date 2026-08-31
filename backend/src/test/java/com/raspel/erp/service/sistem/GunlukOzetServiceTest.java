package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GunlukOzetServiceTest {

    @Mock
    private SirketRepository sirketRepository;
    @Mock
    private StokRepository stokRepository;
    @Mock
    private FaturaRepository faturaRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private GunlukOzetService gunlukOzetService;

    @Test
    void gunlukOzetGonder_emailOlanSirketlereGonderir() {
        Sirket s = Sirket.builder().id(1L).ad("Test").email("sirket@test.com").build();
        when(sirketRepository.findByAktifTrue()).thenReturn(List.of(s));
        when(stokRepository.kritikStoklar(1L)).thenReturn(List.of(Stok.builder().id(1L).build()));
        when(faturaRepository.findVadesiGecen(eq(1L), any(), any(), any())).thenReturn(List.of());

        gunlukOzetService.gunlukOzetGonder();

        verify(emailService).emailGonder(eq("sirket@test.com"), anyString(), anyString());
    }

    @Test
    void gunlukOzetGonder_emailYoksaAtlar() {
        Sirket s = Sirket.builder().id(1L).ad("Test").build();
        when(sirketRepository.findByAktifTrue()).thenReturn(List.of(s));

        gunlukOzetService.gunlukOzetGonder();

        verify(emailService, never()).emailGonder(anyString(), anyString(), anyString());
    }

    @Test
    void gunlukOzetGonder_hataDurumundaDevamEder() {
        Sirket s = Sirket.builder().id(1L).ad("Test").email("sirket@test.com").build();
        when(sirketRepository.findByAktifTrue()).thenReturn(List.of(s));
        when(stokRepository.kritikStoklar(1L)).thenThrow(new RuntimeException("DB hatası"));

        gunlukOzetService.gunlukOzetGonder();

        verify(emailService, never()).emailGonder(anyString(), anyString(), anyString());
    }
}
