package com.raspel.erp.service;

import com.raspel.erp.dto.GorevDTO;
import com.raspel.erp.dto.ProjeDTO;
import com.raspel.erp.entity.Gorev;
import com.raspel.erp.entity.Proje;
import com.raspel.erp.repository.GorevRepository;
import com.raspel.erp.repository.ProjeRepository;
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
class ProjeServiceTest {

    @Mock private ProjeRepository projeRepository;
    @Mock private GorevRepository gorevRepository;
    @InjectMocks private ProjeService projeService;

    private Proje createProje(Long id) {
        Proje p = new Proje();
        p.setId(id);
        p.setAd("Proje " + id);
        p.setBaslangic(LocalDate.now());
        p.setBitis(LocalDate.now().plusMonths(3));
        p.setDurum("DEVAM_EDIYOR");
        p.setSorumlu("Ahmet");
        p.setSirketId(1L);
        p.setOlusturmaTarihi(LocalDateTime.now());
        return p;
    }

    @Test
    void tumunuGetir_returnsAll() {
        when(projeRepository.findBySirketIdOrderByBaslangicDesc(1L)).thenReturn(List.of(createProje(1L)));
        var result = projeService.tumunuGetir(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getir_returnsProje() {
        when(projeRepository.findById(1L)).thenReturn(Optional.of(createProje(1L)));
        var result = projeService.getir(1L);
        assertEquals("Proje 1", result.getAd());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(projeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> projeService.getir(99L));
    }

    @Test
    void olustur_creates() {
        GorevDTO gorev = GorevDTO.builder().ad("Gorev 1").aciklama("Aciklama").atanan("Mehmet")
                .baslangic(LocalDate.now()).bitis(LocalDate.now().plusDays(10)).build();
        ProjeDTO dto = ProjeDTO.builder().ad("Yeni Proje").baslangic(LocalDate.now())
                .bitis(LocalDate.now().plusMonths(2)).sorumlu("Ali").sirketId(1L).gorevler(List.of(gorev)).build();
        Proje saved = createProje(1L);
        saved.setAd("Yeni Proje");
        when(projeRepository.save(any(Proje.class))).thenReturn(saved);
        var result = projeService.olustur(dto);
        assertEquals("Yeni Proje", result.getAd());
    }

    @Test
    void durumGuncelle_updates() {
        Proje proje = createProje(1L);
        when(projeRepository.findById(1L)).thenReturn(Optional.of(proje));
        when(projeRepository.save(any(Proje.class))).thenReturn(proje);
        var result = projeService.durumGuncelle(1L, "TAMAMLANDI");
        assertEquals("TAMAMLANDI", result.getDurum());
    }

    @Test
    void durumGuncelle_throwsWhenNotFound() {
        when(projeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> projeService.durumGuncelle(99L, "TAMAMLANDI"));
    }

    @Test
    void gorevEkle_addsTask() {
        Proje proje = createProje(1L);
        when(projeRepository.findById(1L)).thenReturn(Optional.of(proje));
        GorevDTO dto = GorevDTO.builder().ad("Yeni Gorev").aciklama("Yeni").atanan("Veli")
                .baslangic(LocalDate.now()).bitis(LocalDate.now().plusDays(5)).build();
        var result = projeService.gorevEkle(1L, dto);
        assertNotNull(result);
        verify(gorevRepository).save(any(Gorev.class));
    }

    @Test
    void sil_deletes() {
        when(gorevRepository.findByProjeIdOrderByBaslangicAsc(1L)).thenReturn(List.of());
        projeService.sil(1L);
        verify(projeRepository).deleteById(1L);
    }

    @Test
    void gorevDurumGuncelle_updatesGorev() {
        Gorev gorev = new Gorev();
        gorev.setId(1L);
        gorev.setAd("Test Gorev");
        gorev.setDurum("YAPILACAK");
        when(gorevRepository.findById(1L)).thenReturn(Optional.of(gorev));
        when(gorevRepository.save(any(Gorev.class))).thenReturn(gorev);
        var result = projeService.gorevDurumGuncelle(1L, "TAMAMLANDI");
        assertEquals("TAMAMLANDI", result.getDurum());
    }

    @Test
    void gorevDurumGuncelle_throwsWhenNotFound() {
        when(gorevRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> projeService.gorevDurumGuncelle(99L, "TAMAMLANDI"));
    }
}
