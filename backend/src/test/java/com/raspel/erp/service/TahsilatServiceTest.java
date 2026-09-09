package com.raspel.erp.service;

import com.raspel.erp.dto.finans.TahsilatDTO;
import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.PosTerminali;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.PosTerminaliRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.service.finans.HareketService;
import com.raspel.erp.service.finans.TahsilatService;
import com.raspel.erp.service.sistem.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TahsilatServiceTest {

    @Mock
    private FaturaRepository faturaRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private CariHesapRepository cariHesapRepository;
    @Mock
    private HareketService hareketService;
    @Mock
    private PosTerminaliRepository posTerminaliRepository;
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

    @Test
    void tahsilatGir_karttaPosAdiniCozer() {
        CariHesap c1 = cari(1L, "A Ltd", null);
        Fatura f = fatura(1L, c1, LocalDate.now().minusDays(5), "10000");
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(c1));
        when(faturaRepository.findTahsilatEdilecek(any(), any(), any(), anyList())).thenReturn(List.of(f));
        when(posTerminaliRepository.findById(9L)).thenReturn(Optional.of(PosTerminali.builder().id(9L).ad("Halkbank POS").build()));
        when(hareketService.hareketOlustur(any(), eq(1L))).thenReturn(null);

        tahsilatService.tahsilatGir(1L, new BigDecimal("10000"), "KART", null, null, null,
                LocalDate.now(), 1L, 9L, new BigDecimal("250"), LocalDate.now().plusDays(2));

        ArgumentCaptor<HareketDTO> captor = ArgumentCaptor.forClass(HareketDTO.class);
        verify(hareketService).hareketOlustur(captor.capture(), eq(1L));
        assertEquals(9L, captor.getValue().getPosTerminaliId());
        assertEquals("Halkbank POS", captor.getValue().getPosAd());
        assertEquals(new BigDecimal("250"), captor.getValue().getKomisyonTutar());
    }
}
