package com.raspel.erp.service.envanter;

import com.raspel.erp.dto.envanter.StokDuzeltmeDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokDuzeltme;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.envanter.StokDuzeltmeRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StokDuzeltmeServiceTest {

    @Mock private StokDuzeltmeRepository duzeltmeRepository;
    @Mock private StokRepository stokRepository;
    @Mock private StokHareketRepository stokHareketRepository;
    @InjectMocks private StokDuzeltmeService duzeltmeService;

    @Test
    void duzelt_yeniMiktariUygularVeGecmiseKaydeder() {
        Stok stok = new Stok();
        stok.setId(1L);
        stok.setAd("MDF");
        stok.setMiktar(new BigDecimal("10"));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
        when(duzeltmeRepository.save(any(StokDuzeltme.class))).thenAnswer(inv -> {
            StokDuzeltme d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        StokDuzeltmeDTO sonuc = duzeltmeService.duzelt(
                StokDuzeltmeDTO.builder().stokId(1L).yeniMiktar(new BigDecimal("7")).neden("Sayım farkı").build(),
                1L, 99L);

        assertEquals(0, stok.getMiktar().compareTo(new BigDecimal("7")));
        assertEquals(new BigDecimal("10"), sonuc.getEskiMiktar());
        assertEquals(new BigDecimal("7"), sonuc.getYeniMiktar());
        verify(stokHareketRepository).save(any());
    }

    @Test
    void duzelt_negatifMiktarHataVerir() {
        assertThrows(BusinessException.class,
                () -> duzeltmeService.duzelt(
                        StokDuzeltmeDTO.builder().stokId(1L).yeniMiktar(new BigDecimal("-1")).build(),
                        1L, 99L));
    }
}
