package com.raspel.erp.service;

import com.raspel.erp.dto.sistem.SirketDTO;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.repository.sistem.SirketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.sistem.SirketService;

@ExtendWith(MockitoExtension.class)
class SirketServiceTest {

    @Mock private SirketRepository sirketRepository;
    @InjectMocks private SirketService sirketService;

    private Sirket createSirket(Long id) {
        Sirket s = new Sirket();
        s.setId(id);
        s.setAd("Sirket " + id);
        s.setVergiNo("1234567890");
        s.setVergiDairesi("Istanbul");
        s.setAktif(true);
        s.setOlusturmaTarihi(LocalDateTime.now());
        return s;
    }

    @Test
    void tumunuGetir_returnsAll() {
        when(sirketRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(createSirket(1L), createSirket(2L))));
        var result = sirketService.tumunuGetir(Pageable.unpaged());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void aktifOlanlariGetir_returnsActive() {
        when(sirketRepository.findByAktifTrue()).thenReturn(List.of(createSirket(1L)));
        var result = sirketService.aktifOlanlariGetir();
        assertEquals(1, result.size());
    }

    @Test
    void getir_returnsSirket() {
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(createSirket(1L)));
        var result = sirketService.getir(1L);
        assertEquals("Sirket 1", result.getAd());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(sirketRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> sirketService.getir(99L));
    }

    @Test
    void olustur_creates() {
        SirketDTO dto = SirketDTO.builder().ad("Yeni Sirket").vergiNo("9876543210")
                .vergiDairesi("Ankara").telefon("5550000000").email("info@test.com").build();
        Sirket saved = createSirket(1L);
        saved.setAd("Yeni Sirket");
        when(sirketRepository.save(any(Sirket.class))).thenReturn(saved);
        var result = sirketService.olustur(dto);
        assertEquals("Yeni Sirket", result.getAd());
    }

    @Test
    void guncelle_updates() {
        Sirket existing = createSirket(1L);
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(existing));
        SirketDTO dto = SirketDTO.builder().ad("Guncel Sirket").telefon("5551112222").build();
        when(sirketRepository.save(any(Sirket.class))).thenReturn(existing);
        var result = sirketService.guncelle(1L, dto);
        assertEquals("Guncel Sirket", result.getAd());
    }

    @Test
    void guncelle_throwsWhenNotFound() {
        when(sirketRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> sirketService.guncelle(99L, new SirketDTO()));
    }

    @Test
    void sil_deletes() {
        when(sirketRepository.existsById(1L)).thenReturn(true);
        sirketService.sil(1L);
        verify(sirketRepository).deleteById(1L);
    }

    @Test
    void sil_throwsWhenNotFound() {
        when(sirketRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> sirketService.sil(99L));
    }
}