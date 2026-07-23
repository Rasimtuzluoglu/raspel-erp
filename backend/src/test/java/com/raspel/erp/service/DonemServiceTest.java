package com.raspel.erp.service;

import com.raspel.erp.dto.DonemDTO;
import com.raspel.erp.entity.Donem;
import com.raspel.erp.repository.DonemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonemServiceTest {

    @Mock private DonemRepository donemRepository;
    @InjectMocks private DonemService donemService;

    private Donem createDonem(Long id) {
        Donem d = new Donem();
        d.setId(id);
        d.setSirketId(1L);
        d.setAd("2026-1");
        d.setBaslangic(LocalDate.of(2026, 1, 1));
        d.setBitis(LocalDate.of(2026, 6, 30));
        d.setAktif(true);
        d.setOlusturmaTarihi(LocalDateTime.now());
        return d;
    }

    @Test
    void tumunuGetir_returnsAll() {
        when(donemRepository.findAll()).thenReturn(List.of(createDonem(1L), createDonem(2L)));
        var result = donemService.tumunuGetir();
        assertEquals(2, result.size());
    }

    @Test
    void sirketeGoreGetir_returnsForSirket() {
        when(donemRepository.findBySirketIdOrderByBaslangicDesc(1L)).thenReturn(List.of(createDonem(1L)));
        var result = donemService.sirketeGoreGetir(1L);
        assertEquals(1, result.size());
    }

    @Test
    void aktifDonemler_returnsActive() {
        when(donemRepository.findBySirketIdAndAktifTrue(1L)).thenReturn(List.of(createDonem(1L)));
        var result = donemService.aktifDonemler(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getir_returnsDonem() {
        when(donemRepository.findById(1L)).thenReturn(Optional.of(createDonem(1L)));
        var result = donemService.getir(1L);
        assertEquals("2026-1", result.getAd());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(donemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> donemService.getir(99L));
    }

    @Test
    void olustur_creates() {
        DonemDTO dto = DonemDTO.builder().sirketId(1L).ad("2026-2")
                .baslangic(LocalDate.of(2026, 7, 1)).bitis(LocalDate.of(2026, 12, 31)).build();
        Donem saved = createDonem(1L);
        saved.setAd("2026-2");
        when(donemRepository.save(any(Donem.class))).thenReturn(saved);
        var result = donemService.olustur(dto);
        assertEquals("2026-2", result.getAd());
    }

    @Test
    void guncelle_updates() {
        Donem existing = createDonem(1L);
        when(donemRepository.findById(1L)).thenReturn(Optional.of(existing));
        DonemDTO dto = DonemDTO.builder().ad("Guncel").aktif(false).build();
        when(donemRepository.save(any(Donem.class))).thenReturn(existing);
        var result = donemService.guncelle(1L, dto);
        assertEquals("Guncel", result.getAd());
    }

    @Test
    void guncelle_throwsWhenNotFound() {
        when(donemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> donemService.guncelle(99L, new DonemDTO()));
    }

    @Test
    void sil_deletes() {
        when(donemRepository.existsById(1L)).thenReturn(true);
        donemService.sil(1L);
        verify(donemRepository).deleteById(1L);
    }

    @Test
    void sil_throwsWhenNotFound() {
        when(donemRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> donemService.sil(99L));
    }
}
