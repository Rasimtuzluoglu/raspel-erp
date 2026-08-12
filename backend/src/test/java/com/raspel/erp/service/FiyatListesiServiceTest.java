package com.raspel.erp.service;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.FiyatListesiDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.ticaret.FiyatListesi;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.ticaret.FiyatListesiRepository;
import com.raspel.erp.service.ticaret.FiyatListesiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FiyatListesiServiceTest {

    @Mock private FiyatListesiRepository fiyatListesiRepository;
    @Mock private StokRepository stokRepository;
    @Mock private TenantChecker tenantChecker;
    @InjectMocks private FiyatListesiService fiyatListesiService;

    private Stok createStok(Long id) {
        Stok s = new Stok();
        s.setId(id);
        s.setAd("Kalem");
        s.setMiktar(BigDecimal.ZERO);
        s.setFiyat(BigDecimal.ZERO);
        return s;
    }

    private FiyatListesi createFiyatListesi(Long id) {
        return FiyatListesi.builder()
                .id(id)
                .stok(createStok(1L))
                .alisFiyat(BigDecimal.valueOf(10))
                .satisFiyat(BigDecimal.valueOf(20))
                .sirketId(1L)
                .aciklama("Test")
                .build();
    }

    @Test
    void tumunuGetir_returnsPage() {
        when(fiyatListesiRepository.findBySirketId(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createFiyatListesi(1L))));
        var result = fiyatListesiService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, result.getContent().size());
        assertEquals("Kalem", result.getContent().get(0).getStokAdi());
    }

    @Test
    void getir_returnsDto() {
        when(fiyatListesiRepository.findById(1L)).thenReturn(Optional.of(createFiyatListesi(1L)));
        var result = fiyatListesiService.getir(1L);
        assertEquals("Kalem", result.getStokAdi());
        verify(tenantChecker).check(eq(1L), anyString());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(fiyatListesiRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> fiyatListesiService.getir(99L));
    }

    @Test
    void olustur_creates() {
        when(stokRepository.findById(1L)).thenReturn(Optional.of(createStok(1L)));
        FiyatListesi saved = createFiyatListesi(1L);
        when(fiyatListesiRepository.save(any(FiyatListesi.class))).thenReturn(saved);
        FiyatListesiDTO dto = FiyatListesiDTO.builder()
                .stokId(1L).alisFiyat(BigDecimal.valueOf(10)).satisFiyat(BigDecimal.valueOf(20)).build();
        var result = fiyatListesiService.olustur(dto, 1L);
        assertEquals("Kalem", result.getStokAdi());
        assertEquals(1L, result.getSirketId());
        verify(fiyatListesiRepository).save(any(FiyatListesi.class));
    }

    @Test
    void olustur_throwsWhenStokNotFound() {
        when(stokRepository.findById(99L)).thenReturn(Optional.empty());
        FiyatListesiDTO dto = FiyatListesiDTO.builder().stokId(99L).build();
        assertThrows(RuntimeException.class, () -> fiyatListesiService.olustur(dto, 1L));
    }

    @Test
    void sil_deletes() {
        when(fiyatListesiRepository.findById(1L)).thenReturn(Optional.of(createFiyatListesi(1L)));
        fiyatListesiService.sil(1L);
        verify(fiyatListesiRepository).deleteById(1L);
    }

    @Test
    void sil_throwsWhenNotFound() {
        when(fiyatListesiRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> fiyatListesiService.sil(99L));
    }
}
