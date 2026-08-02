package com.raspel.erp.service.finans;

import com.raspel.erp.dto.finans.ButceDTO;
import com.raspel.erp.entity.finans.Butce;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.ButceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ButceServiceTest {

    @Mock private ButceRepository butceRepository;
    @InjectMocks private ButceService butceService;

    private Butce ornekButce(Long id) {
        return Butce.builder()
                .id(id).ad("2026 Bütçe").yil(2026).ay(1)
                .tutar(new BigDecimal("50000")).tur("GIDER")
                .kategori("Pazarlama").sirketId(1L).olusturmaTarihi(LocalDateTime.now())
                .build();
    }

    @Test
    void tumunuGetir_returnsPage() {
        when(butceRepository.findBySirketIdOrderByYilDescAyDesc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornekButce(1L))));
        var sonuc = butceService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, sonuc.getContent().size());
        assertEquals("2026 Bütçe", sonuc.getContent().get(0).getAd());
    }

    @Test
    void getir_returnsById() {
        when(butceRepository.findById(1L)).thenReturn(Optional.of(ornekButce(1L)));
        var sonuc = butceService.getir(1L);
        assertEquals(2026, sonuc.getYil());
    }

    @Test
    void getir_notFound_throws() {
        when(butceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> butceService.getir(99L));
    }

    @Test
    void olustur_creates() {
        ButceDTO dto = ButceDTO.builder().ad("Yeni Bütçe").yil(2026).ay(3)
                .tutar(new BigDecimal("10000")).tur("GELIR").build();
        when(butceRepository.save(any(Butce.class))).thenAnswer(inv -> {
            Butce b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });
        var sonuc = butceService.olustur(dto, 1L);
        assertEquals("Yeni Bütçe", sonuc.getAd());
        assertEquals(1L, sonuc.getSirketId());
    }

    @Test
    void guncelle_updatesOnlyNonNull() {
        Butce butce = ornekButce(1L);
        when(butceRepository.findById(1L)).thenReturn(Optional.of(butce));
        when(butceRepository.save(any(Butce.class))).thenAnswer(inv -> inv.getArgument(0));

        ButceDTO dto = ButceDTO.builder().ad("Güncel Bütçe").build();
        var sonuc = butceService.guncelle(1L, dto);
        assertEquals("Güncel Bütçe", sonuc.getAd());
        assertEquals(2026, sonuc.getYil());
    }

    @Test
    void sil_deletes() {
        when(butceRepository.existsById(1L)).thenReturn(true);
        butceService.sil(1L);
        verify(butceRepository).deleteById(1L);
    }

    @Test
    void sil_notFound_throws() {
        when(butceRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> butceService.sil(99L));
    }
}
