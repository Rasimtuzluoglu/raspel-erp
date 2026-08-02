package com.raspel.erp.service.finans;

import com.raspel.erp.dto.finans.MasrafDTO;
import com.raspel.erp.entity.finans.Masraf;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.MasrafRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasrafServiceTest {

    @Mock private MasrafRepository masrafRepository;
    @InjectMocks private MasrafService masrafService;

    private Masraf ornekMasraf(Long id) {
        return Masraf.builder()
                .id(id).tarih(LocalDate.now()).tutar(new BigDecimal("750"))
                .aciklama("Kırtasiye").kategori("Ofis").sirketId(1L)
                .olusturmaTarihi(LocalDateTime.now()).build();
    }

    @Test
    void tumunuGetir_returnsPage() {
        when(masrafRepository.findBySirketIdOrderByTarihDesc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornekMasraf(1L))));
        var sonuc = masrafService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, sonuc.getContent().size());
    }

    @Test
    void getir_returnsById() {
        when(masrafRepository.findById(1L)).thenReturn(Optional.of(ornekMasraf(1L)));
        var sonuc = masrafService.getir(1L);
        assertEquals("Kırtasiye", sonuc.getAciklama());
    }

    @Test
    void getir_notFound_throws() {
        when(masrafRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> masrafService.getir(99L));
    }

    @Test
    void olustur_creates() {
        MasrafDTO dto = MasrafDTO.builder().tarih(LocalDate.now())
                .tutar(new BigDecimal("300")).aciklama("Yakıt").kategori("Araç").build();
        when(masrafRepository.save(any(Masraf.class))).thenAnswer(inv -> {
            Masraf m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });
        var sonuc = masrafService.olustur(dto, 1L);
        assertEquals("Yakıt", sonuc.getAciklama());
        assertEquals(1L, sonuc.getSirketId());
    }

    @Test
    void guncelle_updates() {
        Masraf masraf = ornekMasraf(1L);
        when(masrafRepository.findById(1L)).thenReturn(Optional.of(masraf));
        when(masrafRepository.save(any(Masraf.class))).thenAnswer(inv -> inv.getArgument(0));

        MasrafDTO dto = MasrafDTO.builder().tutar(new BigDecimal("999")).build();
        var sonuc = masrafService.guncelle(1L, dto);
        assertEquals(0, sonuc.getTutar().compareTo(new BigDecimal("999")));
    }

    @Test
    void sil_deletes() {
        when(masrafRepository.existsById(1L)).thenReturn(true);
        masrafService.sil(1L);
        verify(masrafRepository).deleteById(1L);
    }

    @Test
    void sil_notFound_throws() {
        when(masrafRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> masrafService.sil(99L));
    }
}
