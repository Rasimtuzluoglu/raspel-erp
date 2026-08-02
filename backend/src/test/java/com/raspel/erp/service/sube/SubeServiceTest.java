package com.raspel.erp.service.sube;

import com.raspel.erp.dto.sube.SubeDTO;
import com.raspel.erp.entity.sube.Sube;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sube.SubeRepository;
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

@ExtendWith(MockitoExtension.class)
class SubeServiceTest {

    @Mock private SubeRepository subeRepository;
    @InjectMocks private SubeService subeService;

    private Sube ornekSube(Long id) {
        return Sube.builder()
                .id(id).ad("Merkez Şube").adres("İstanbul")
                .telefon("0212 000 00 00").yetkili("Ahmet")
                .sirketId(1L).aktif(true).olusturmaTarihi(LocalDateTime.now())
                .build();
    }

    @Test
    void tumunuGetir_returnsPage() {
        when(subeRepository.findBySirketIdOrderByAdAsc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornekSube(1L))));
        var sonuc = subeService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, sonuc.getContent().size());
    }

    @Test
    void aktifSubeler_returnsOnlyActive() {
        when(subeRepository.findBySirketIdAndAktifTrue(1L)).thenReturn(List.of(ornekSube(1L)));
        var sonuc = subeService.aktifSubeler(1L);
        assertEquals(1, sonuc.size());
    }

    @Test
    void getir_returnsById() {
        when(subeRepository.findById(1L)).thenReturn(Optional.of(ornekSube(1L)));
        var sonuc = subeService.getir(1L);
        assertEquals("Merkez Şube", sonuc.getAd());
    }

    @Test
    void getir_notFound_throws() {
        when(subeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> subeService.getir(99L));
    }

    @Test
    void olustur_creates() {
        SubeDTO dto = SubeDTO.builder().ad("Yeni Şube").sirketId(1L).build();
        when(subeRepository.save(any(Sube.class))).thenAnswer(inv -> {
            Sube s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        var sonuc = subeService.olustur(dto);
        assertEquals("Yeni Şube", sonuc.getAd());
    }

    @Test
    void guncelle_updates() {
        Sube sube = ornekSube(1L);
        when(subeRepository.findById(1L)).thenReturn(Optional.of(sube));
        when(subeRepository.save(any(Sube.class))).thenAnswer(inv -> inv.getArgument(0));

        SubeDTO dto = SubeDTO.builder().adres("Ankara").build();
        var sonuc = subeService.guncelle(1L, dto);
        assertEquals("Ankara", sonuc.getAdres());
        assertEquals("Merkez Şube", sonuc.getAd());
    }

    @Test
    void sil_deletes() {
        when(subeRepository.existsById(1L)).thenReturn(true);
        subeService.sil(1L);
        verify(subeRepository).deleteById(1L);
    }
}
