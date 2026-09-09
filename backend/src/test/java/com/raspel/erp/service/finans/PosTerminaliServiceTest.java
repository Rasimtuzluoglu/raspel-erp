package com.raspel.erp.service.finans;

import com.raspel.erp.dto.finans.PosTerminaliDTO;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.finans.PosTerminali;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.finans.PosTerminaliRepository;
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
class PosTerminaliServiceTest {

    @Mock private PosTerminaliRepository posRepository;
    @Mock private BankaRepository bankaRepository;
    @Mock private HareketRepository hareketRepository;
    @InjectMocks private PosTerminaliService posService;

    @Test
    void olustur_bosAdHataVerir() {
        assertThrows(BusinessException.class, () -> posService.olustur(PosTerminaliDTO.builder().ad(" ").build(), 1L));
    }

    @Test
    void olustur_kaydeder() {
        when(posRepository.save(any(PosTerminali.class))).thenAnswer(inv -> {
            PosTerminali p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        PosTerminaliDTO sonuc = posService.olustur(PosTerminaliDTO.builder().ad("Halkbank POS").komisyonOrani(new BigDecimal("2.50")).build(), 1L);
        assertEquals(1L, sonuc.getId());
        assertEquals("Halkbank POS", sonuc.getAd());
    }

    @Test
    void liste_bankaAdiniDoldurur() {
        PosTerminali p = PosTerminali.builder().id(1L).sirketId(1L).ad("Halkbank POS").bankaId(7L).komisyonOrani(BigDecimal.ZERO).aktif(true).build();
        when(posRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of(p));
        when(bankaRepository.findById(7L)).thenReturn(Optional.of(Banka.builder().id(7L).ad("Halkbank").build()));

        var sonuc = posService.liste(1L);
        assertEquals("Halkbank", sonuc.get(0).getBankaAd());
    }

    @Test
    void ozet_posBazliToplamHesaplar() {
        PosTerminali p = PosTerminali.builder().id(1L).sirketId(1L).ad("Halkbank POS").build();
        when(posRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of(p));

        LocalDate bugun = LocalDate.now();
        Hareket h1 = Hareket.builder().tutar(new BigDecimal("1000")).komisyonTutar(new BigDecimal("25")).hareketTarihi(bugun).build();
        Hareket h2 = Hareket.builder().tutar(new BigDecimal("2000")).komisyonTutar(new BigDecimal("50")).hareketTarihi(bugun.minusDays(3)).build();
        when(hareketRepository.findBySirketIdAndPosTerminaliIdOrderByHareketTarihiDesc(1L, 1L)).thenReturn(List.of(h1, h2));

        var ozetler = posService.ozet(1L);

        assertEquals(1, ozetler.size());
        assertEquals(new BigDecimal("1000"), ozetler.get(0).getBugunTutar());
        assertEquals(new BigDecimal("3000"), ozetler.get(0).getToplamTutar());
        assertEquals(new BigDecimal("75"), ozetler.get(0).getToplamKomisyon());
    }
}
