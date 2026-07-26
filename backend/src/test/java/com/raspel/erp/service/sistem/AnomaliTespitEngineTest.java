package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AnomaliDTO;
import com.raspel.erp.entity.CariHesap;
import com.raspel.erp.entity.Fatura;
import com.raspel.erp.entity.Hareket;
import com.raspel.erp.repository.FaturaRepository;
import com.raspel.erp.repository.HareketRepository;
import com.raspel.erp.repository.StokHareketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomaliTespitEngineTest {

    @Mock
    private FaturaRepository faturaRepository;

    @Mock
    private HareketRepository hareketRepository;

    @Mock
    private StokHareketRepository stokHareketRepository;

    @InjectMocks
    private AnomaliTespitEngine anomaliTespitEngine;

    @Test
    void testAnomalileriTara_MukerrerFaturaTespitEder() {
        CariHesap cari = CariHesap.builder().id(10L).ad("Test Cari").build();
        Fatura f1 = Fatura.builder().id(1L).cariHesap(cari).genelToplam(BigDecimal.valueOf(5000)).build();
        Fatura f2 = Fatura.builder().id(2L).cariHesap(cari).genelToplam(BigDecimal.valueOf(5000)).build();

        when(faturaRepository.findBySirketIdOrderByTarihDesc(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(f1, f2)));
        when(hareketRepository.findBySirketIdOrderByHareketTarihiDesc(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<AnomaliDTO> list = anomaliTespitEngine.anomalileriTara(100L);

        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(a -> a.getTur().equals("MUKERRER_FATURA")));
    }

    @Test
    void testAnomalileriTara_AnormalYuksekTutarTespitEder() {
        CariHesap cari = CariHesap.builder().id(10L).ad("Test Cari").build();
        Fatura f1 = Fatura.builder().id(1L).cariHesap(cari).faturaNumarasi("FTR-999").genelToplam(BigDecimal.valueOf(75000)).build();

        when(faturaRepository.findBySirketIdOrderByTarihDesc(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(f1)));
        when(hareketRepository.findBySirketIdOrderByHareketTarihiDesc(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<AnomaliDTO> list = anomaliTespitEngine.anomalileriTara(100L);

        assertNotNull(list);
        assertTrue(list.stream().anyMatch(a -> a.getTur().equals("ANORMAL_MASRAF")));
    }
}
