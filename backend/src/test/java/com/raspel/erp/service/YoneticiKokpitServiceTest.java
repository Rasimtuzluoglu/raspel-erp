package com.raspel.erp.service;

import com.raspel.erp.dto.sistem.SirketHedefDTO;
import com.raspel.erp.dto.sistem.YoneticiKokpitDTO;
import com.raspel.erp.entity.sistem.SirketHedef;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.finans.MasrafRepository;
import com.raspel.erp.repository.sistem.SirketHedefRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.service.sistem.YoneticiKokpitService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YoneticiKokpitServiceTest {

    @Mock private SirketHedefRepository sirketHedefRepository;
    @Mock private FaturaRepository faturaRepository;
    @Mock private MasrafRepository masrafRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private KasaRepository kasaRepository;
    @Mock private BankaRepository bankaRepository;
    @InjectMocks private YoneticiKokpitService kokpitService;

    @Test
    void shouldGetKokpitVerileri() {
        SirketHedef hedef = SirketHedef.builder()
                .sirketId(1L)
                .yil(2026)
                .ay(8)
                .hedefCiro(BigDecimal.valueOf(100000))
                .hedefKar(BigDecimal.valueOf(30000))
                .build();

        when(sirketHedefRepository.findBySirketIdAndYilAndAy(eq(1L), eq(2026), eq(8)))
                .thenReturn(Optional.of(hedef));
        when(faturaRepository.findBySirketIdAndTarihBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(masrafRepository.findBySirketIdAndTarihBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(kasaRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of(
                com.raspel.erp.entity.finans.Kasa.builder().bakiye(BigDecimal.valueOf(15000)).build()
        ));
        when(bankaRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of(
                com.raspel.erp.entity.finans.Banka.builder().bakiye(BigDecimal.valueOf(85000)).build()
        ));
        when(cariHesapRepository.toplamPozitifBakiyeBySirketId(1L)).thenReturn(BigDecimal.valueOf(50000));
        when(cariHesapRepository.toplamNegatifBakiyeBySirketId(1L)).thenReturn(BigDecimal.valueOf(-20000));
        when(faturaRepository.findVadesiGecen(eq(1L), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        YoneticiKokpitDTO result = kokpitService.getKokpitVerileri(1L, 2026, 8);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(100000), result.getHedefCiro());
        assertEquals(BigDecimal.valueOf(100000), result.getKasaBankaToplam());
        assertEquals(BigDecimal.valueOf(50000), result.getToplamAlacak());
        assertEquals(BigDecimal.valueOf(20000), result.getToplamBorc());
        assertEquals(31, result.getGunlukCiroTrendi().size());
    }

    @Test
    void shouldSaveHedef() {
        when(sirketHedefRepository.findBySirketIdAndYilAndAy(1L, 2026, 8))
                .thenReturn(Optional.empty());
        when(sirketHedefRepository.save(any(SirketHedef.class))).thenAnswer(inv -> {
            SirketHedef h = inv.getArgument(0);
            h.setId(10L);
            return h;
        });

        SirketHedefDTO dto = SirketHedefDTO.builder()
                .yil(2026)
                .ay(8)
                .hedefCiro(BigDecimal.valueOf(500000))
                .hedefKar(BigDecimal.valueOf(150000))
                .build();

        SirketHedefDTO result = kokpitService.hedefKaydet(dto, 1L);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(500000), result.getHedefCiro());
        verify(sirketHedefRepository).save(any(SirketHedef.class));
    }
}
