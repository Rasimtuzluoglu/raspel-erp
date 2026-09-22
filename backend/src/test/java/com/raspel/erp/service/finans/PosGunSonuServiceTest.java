package com.raspel.erp.service.finans;

import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.finans.PosTerminali;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.finans.PosGunSonuRepository;
import com.raspel.erp.repository.finans.PosTerminaliRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class PosGunSonuServiceTest {

    @Mock private PosTerminaliRepository posRepository;
    @Mock private HareketRepository hareketRepository;
    @Mock private BankaRepository bankaRepository;
    @Mock private PosGunSonuRepository gunSonuRepository;
    @Mock private SirketRepository sirketRepository;
    @InjectMocks private PosGunSonuService gunSonuService;

    @Test
    void gunSonuIsle_tutariBankayaAktarirVeKaydeder() {
        PosTerminali p = PosTerminali.builder().id(1L).sirketId(1L).ad("Halkbank POS").bankaId(3L).aktif(true).build();
        when(posRepository.findBySirketIdAndAktifTrueOrderByAd(1L)).thenReturn(List.of(p));
        when(gunSonuRepository.existsByPosIdAndTarih(1L, LocalDate.now())).thenReturn(false);
        when(hareketRepository.findBySirketIdAndPosTerminaliIdOrderByHareketTarihiDesc(1L, 1L))
                .thenReturn(List.of(
                        Hareket.builder().tutar(new BigDecimal("1000")).komisyonTutar(new BigDecimal("20")).hareketTarihi(LocalDate.now()).build(),
                        Hareket.builder().tutar(new BigDecimal("500")).hareketTarihi(LocalDate.now().minusDays(1)).build()));
        Banka banka = Banka.builder().id(3L).ad("Halkbank").bakiye(BigDecimal.ZERO).sirketId(1L).build();
        when(bankaRepository.findById(3L)).thenReturn(Optional.of(banka));

        var sonuc = gunSonuService.gunSonuIsle(1L);

        assertEquals(1, sonuc.size());
        // Bankaya komisyon düşülerek NET tutar geçer: 1000 - 20 = 980.
        assertEquals(new BigDecimal("980"), banka.getBakiye());
        verify(gunSonuRepository).saveAndFlush(any());
    }

    @Test
    void gunSonuIsle_zatenIslenmisseAtlar() {
        PosTerminali p = PosTerminali.builder().id(1L).sirketId(1L).ad("Halkbank POS").bankaId(3L).aktif(true).build();
        when(posRepository.findBySirketIdAndAktifTrueOrderByAd(1L)).thenReturn(List.of(p));
        when(gunSonuRepository.existsByPosIdAndTarih(1L, LocalDate.now())).thenReturn(true);

        var sonuc = gunSonuService.gunSonuIsle(1L);

        assertTrue(sonuc.isEmpty());
        verify(bankaRepository, never()).findById(anyLong());
    }

    @Test
    void gunSonuIsle_bankaBulunamazsaHataFirlatirVeKayitYapmaz() {
        PosTerminali p = PosTerminali.builder().id(1L).sirketId(1L).ad("Halkbank POS").bankaId(3L).aktif(true).build();
        when(posRepository.findBySirketIdAndAktifTrueOrderByAd(1L)).thenReturn(List.of(p));
        when(gunSonuRepository.existsByPosIdAndTarih(1L, LocalDate.now())).thenReturn(false);
        when(hareketRepository.findBySirketIdAndPosTerminaliIdOrderByHareketTarihiDesc(1L, 1L))
                .thenReturn(List.of(Hareket.builder().tutar(new BigDecimal("1000")).hareketTarihi(LocalDate.now()).build()));
        when(bankaRepository.findById(3L)).thenReturn(java.util.Optional.empty());

        com.raspel.erp.exception.BusinessException ex = assertThrows(
                com.raspel.erp.exception.BusinessException.class, () -> gunSonuService.gunSonuIsle(1L));

        assertTrue(ex.getMessage().toLowerCase().contains("banka"));
        verify(gunSonuRepository, never()).saveAndFlush(any());
    }
}
