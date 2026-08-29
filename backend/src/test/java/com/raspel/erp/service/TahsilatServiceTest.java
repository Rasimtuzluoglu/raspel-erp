package com.raspel.erp.service;

import com.raspel.erp.dto.finans.TahsilatDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.service.finans.TahsilatService;
import com.raspel.erp.service.sistem.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TahsilatServiceTest {

    @Mock
    private FaturaRepository faturaRepository;
    @Mock
    private EmailService emailService;
    @InjectMocks
    private TahsilatService tahsilatService;

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

        when(faturaRepository.findTahsilatEdilecek(any(), any(), any(), anyList())).thenReturn(faturalar);

        int sent = tahsilatService.hatirlat(1L, 1L);

        assertEquals(2, sent);
        verify(emailService, times(2)).odemeHatimlaticiGonder(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void hatirlat_throwsWhenNoEmail() {
        CariHesap c1 = cari(1L, "A Ltd", null);
        List<Fatura> faturalar = List.of(
                fatura(1L, c1, LocalDate.now().minusDays(5), "1000"));

        when(faturaRepository.findTahsilatEdilecek(any(), any(), any(), anyList())).thenReturn(faturalar);

        assertThrows(BusinessException.class, () -> tahsilatService.hatirlat(1L, 1L));
    }
}
