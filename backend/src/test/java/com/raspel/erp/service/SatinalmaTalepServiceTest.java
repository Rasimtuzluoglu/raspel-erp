package com.raspel.erp.service;

import com.raspel.erp.dto.SatinalmaTalepDTO;
import com.raspel.erp.dto.SatinalmaTalepKalemDTO;
import com.raspel.erp.entity.SatinalmaTalep;
import com.raspel.erp.repository.SatinalmaTalepKalemRepository;
import com.raspel.erp.repository.SatinalmaTalepRepository;
import com.raspel.erp.repository.StokRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SatinalmaTalepServiceTest {

    @Mock private SatinalmaTalepRepository talepRepository;
    @Mock private SatinalmaTalepKalemRepository kalemRepository;
    @Mock private StokRepository stokRepository;
    @InjectMocks private SatinalmaTalepService satinalmaTalepService;

    private SatinalmaTalep createTalep(Long id) {
        SatinalmaTalep t = new SatinalmaTalep();
        t.setId(id);
        t.setTalepNo("TLP-000" + id);
        t.setTarih(LocalDate.now());
        t.setTalepEden("Ahmet");
        t.setDepartman("Uretim");
        t.setDurum("TASLAK");
        t.setSirketId(1L);
        t.setOlusturmaTarihi(LocalDateTime.now());
        return t;
    }

    @Test
    void tumunuGetir_returnsAll() {
        when(talepRepository.findBySirketIdOrderByTarihDesc(1L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(createTalep(1L))));
        var result = satinalmaTalepService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getir_returnsTalep() {
        when(talepRepository.findById(1L)).thenReturn(Optional.of(createTalep(1L)));
        var result = satinalmaTalepService.getir(1L);
        assertEquals("TLP-0001", result.getTalepNo());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(talepRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> satinalmaTalepService.getir(99L));
    }

    @Test
    void olustur_creates() {
        SatinalmaTalepKalemDTO kalem = SatinalmaTalepKalemDTO.builder().stokId(1L)
                .aciklama("K").miktar(BigDecimal.valueOf(10)).birim("Adet").tahminiBirimFiyat(BigDecimal.valueOf(50)).build();
        SatinalmaTalepDTO dto = SatinalmaTalepDTO.builder().talepNo("TLP-999").tarih(LocalDate.now())
                .talepEden("Mehmet").departman("IT").aciklama("Test").sirketId(1L).kalemler(List.of(kalem)).build();
        SatinalmaTalep saved = createTalep(1L);
        when(talepRepository.save(any(SatinalmaTalep.class))).thenReturn(saved);
        var result = satinalmaTalepService.olustur(dto);
        assertNotNull(result);
    }

    @Test
    void durumGuncelle_updates() {
        SatinalmaTalep talep = createTalep(1L);
        when(talepRepository.findById(1L)).thenReturn(Optional.of(talep));
        when(talepRepository.save(any(SatinalmaTalep.class))).thenReturn(talep);
        var result = satinalmaTalepService.durumGuncelle(1L, "ONAYLANDI");
        assertEquals("ONAYLANDI", result.getDurum());
    }

    @Test
    void durumGuncelle_throwsWhenNotFound() {
        when(talepRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> satinalmaTalepService.durumGuncelle(99L, "ONAYLANDI"));
    }

    @Test
    void sil_deletes() {
        when(talepRepository.existsById(1L)).thenReturn(true);
        satinalmaTalepService.sil(1L);
        verify(kalemRepository).deleteByTalepId(1L);
        verify(talepRepository).deleteById(1L);
    }
}
