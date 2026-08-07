package com.raspel.erp.service;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sistem.NotDTO;
import com.raspel.erp.entity.sistem.Not;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.NotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.sistem.NotService;

@ExtendWith(MockitoExtension.class)
class NotServiceTest {

    @Mock private NotRepository notRepository;
    @Mock private TenantChecker tenantChecker;
    @InjectMocks private NotService notService;

    private Not createNot(Long id) {
        Not n = new Not();
        n.setId(id);
        n.setBaslik("Not " + id);
        n.setIcerik("İçerik " + id);
        n.setOnemDerecesi("NORMAL");
        n.setKullaniciId(1L);
        n.setSirketId(1L);
        n.setOlusturmaTarihi(LocalDateTime.now());
        return n;
    }

    @Test
    void tumunuGetir_returnsPage() {
        when(notRepository.findBySirketIdOrderByOlusturmaTarihiDesc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createNot(1L), createNot(2L))));
        var result = notService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void kullaniciNotlari_returnsList() {
        when(notRepository.findBySirketIdAndKullaniciIdOrderByOlusturmaTarihiDesc(1L, 1L))
                .thenReturn(List.of(createNot(1L)));
        var result = notService.kullaniciNotlari(1L, 1L);
        assertEquals(1, result.size());
    }

    @Test
    void idyeGoreGetir_returnsNot() {
        when(notRepository.findById(1L)).thenReturn(Optional.of(createNot(1L)));
        var result = notService.idyeGoreGetir(1L);
        assertEquals("Not 1", result.getBaslik());
    }

    @Test
    void idyeGoreGetir_notFound_throws() {
        when(notRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> notService.idyeGoreGetir(99L));
    }

    @Test
    void olustur_createsWithDefaults() {
        NotDTO dto = NotDTO.builder().baslik("Yeni Not").icerik("İçerik").build();
        Not saved = createNot(1L);
        saved.setBaslik("Yeni Not");
        when(notRepository.save(any(Not.class))).thenReturn(saved);
        var result = notService.olustur(dto, 1L, 1L);
        assertEquals("Yeni Not", result.getBaslik());
        assertEquals(1L, result.getKullaniciId());
    }

    @Test
    void guncelle_updates() {
        Not not = createNot(1L);
        when(notRepository.findById(1L)).thenReturn(Optional.of(not));
        when(notRepository.save(any(Not.class))).thenAnswer(inv -> inv.getArgument(0));
        NotDTO dto = NotDTO.builder().baslik("Güncel").icerik("Yeni içerik").onemDerecesi("YUKSEK").build();
        var result = notService.guncelle(1L, dto);
        assertEquals("Güncel", result.getBaslik());
        assertEquals("YUKSEK", result.getOnemDerecesi());
    }

    @Test
    void sil_deletes() {
        when(notRepository.findById(1L)).thenReturn(Optional.of(createNot(1L)));
        notService.sil(1L);
        verify(notRepository).deleteById(1L);
    }
}