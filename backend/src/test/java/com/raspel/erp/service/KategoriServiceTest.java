package com.raspel.erp.service;

import com.raspel.erp.dto.KategoriDTO;
import com.raspel.erp.entity.GelirGiderKategori;
import com.raspel.erp.repository.KategoriRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KategoriServiceTest {

    @Mock private KategoriRepository kategoriRepository;
    @InjectMocks private KategoriService kategoriService;

    private GelirGiderKategori createKategori(Long id) {
        GelirGiderKategori k = new GelirGiderKategori();
        k.setId(id);
        k.setAd("Kategori " + id);
        k.setTur("GELIR");
        k.setOlusturmaTarihi(LocalDateTime.now());
        return k;
    }

    @Test
    void tumunuGetir_returnsAll() {
        when(kategoriRepository.findBySirketId(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(createKategori(1L), createKategori(2L))));
        var result = kategoriService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void turuGetir_returnsByTur() {
        when(kategoriRepository.findByTurOrderByAd("GIDER")).thenReturn(List.of(createKategori(1L)));
        var result = kategoriService.turuGetir("GIDER");
        assertEquals(1, result.size());
    }

    @Test
    void olustur_creates() {
        KategoriDTO dto = KategoriDTO.builder().ad("Yeni Kategori").tur("GELIR").build();
        GelirGiderKategori saved = createKategori(1L);
        saved.setAd("Yeni Kategori");
        when(kategoriRepository.save(any(GelirGiderKategori.class))).thenReturn(saved);
        var result = kategoriService.olustur(dto, 1L);
        assertEquals("Yeni Kategori", result.getAd());
    }

    @Test
    void sil_deletes() {
        kategoriService.sil(1L);
        verify(kategoriRepository).deleteById(1L);
    }
}
