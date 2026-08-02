package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.CariHesap;
import com.raspel.erp.entity.Fatura;
import com.raspel.erp.repository.FaturaRepository;
import com.raspel.erp.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HatirlaticiServiceTest {

    @Mock private FaturaRepository faturaRepository;
    @Mock private EmailService emailService;

    @InjectMocks private HatirlaticiService hatirlaticiService;

    private CariHesap cari;

    @BeforeEach
    void setUp() {
        cari = new CariHesap();
        cari.setId(1L);
        cari.setAd("Test Müşteri");
        cari.setEmail("musteri@ornek.com");
        cari.setOdemeVadesi(30);
    }

    @Test
    void vadesiGecenFaturayaHatirlatmaGonderir() {
        Fatura fatura = Fatura.builder()
                .id(1L).faturaNumarasi("FTR-1").tur(Fatura.FaturaTur.SATIS)
                .tarih(LocalDate.now().minusDays(40))
                .genelToplam(BigDecimal.valueOf(1000))
                .kalanTutar(BigDecimal.valueOf(1000))
                .cariHesap(cari)
                .build();
        when(faturaRepository.findByTurAndOdemeDurumuNotIn(eq(Fatura.FaturaTur.SATIS), any())).thenReturn(List.of(fatura));

        hatirlaticiService.vadesiGecenHatirlaticiGonder();

        verify(emailService, times(1)).odemeHatimlaticiGonder(eq("musteri@ornek.com"), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void vadesiGecmemisFaturayaHatirlatmaGondermez() {
        Fatura fatura = Fatura.builder()
                .id(1L).faturaNumarasi("FTR-2").tur(Fatura.FaturaTur.SATIS)
                .tarih(LocalDate.now())
                .genelToplam(BigDecimal.valueOf(500))
                .kalanTutar(BigDecimal.valueOf(500))
                .cariHesap(cari)
                .build();
        when(faturaRepository.findByTurAndOdemeDurumuNotIn(eq(Fatura.FaturaTur.SATIS), any())).thenReturn(List.of(fatura));

        hatirlaticiService.vadesiGecenHatirlaticiGonder();

        verify(emailService, never()).odemeHatimlaticiGonder(any(), any(), any(), any(), any(), any());
    }

    @Test
    void epostaAdresiOlmayanCariIcinGondermez() {
        cari.setEmail(null);
        Fatura fatura = Fatura.builder()
                .id(1L).faturaNumarasi("FTR-3").tur(Fatura.FaturaTur.SATIS)
                .tarih(LocalDate.now().minusDays(60))
                .genelToplam(BigDecimal.valueOf(100))
                .kalanTutar(BigDecimal.valueOf(100))
                .cariHesap(cari)
                .build();
        when(faturaRepository.findByTurAndOdemeDurumuNotIn(eq(Fatura.FaturaTur.SATIS), any())).thenReturn(List.of(fatura));

        hatirlaticiService.vadesiGecenHatirlaticiGonder();

        verify(emailService, never()).odemeHatimlaticiGonder(any(), any(), any(), any(), any(), any());
    }
}
