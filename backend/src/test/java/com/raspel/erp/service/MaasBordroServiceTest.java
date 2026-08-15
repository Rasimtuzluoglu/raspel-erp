package com.raspel.erp.service;

import com.raspel.erp.dto.ik.MaasBordroDTO;
import com.raspel.erp.entity.ik.MaasBordro;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ik.MaasBordroRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.service.ik.MaasBordroService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaasBordroServiceTest {

    @Mock private MaasBordroRepository maasBordroRepository;
    @Mock private PersonelRepository personelRepository;
    @InjectMocks private MaasBordroService maasBordroService;

    private Personel createPersonel() {
        Personel p = new Personel();
        p.setId(1L);
        p.setAd("Ahmet");
        p.setSoyad("Yilmaz");
        return p;
    }

    private MaasBordro createBordro(Long id) {
        return MaasBordro.builder()
                .id(id)
                .personel(createPersonel())
                .yil(2026)
                .ay(7)
                .brutMaas(new BigDecimal("30000.00"))
                .kesintiler(new BigDecimal("9000.00"))
                .netMaas(new BigDecimal("21000.00"))
                .odemeTarihi(LocalDate.of(2026, 7, 30))
                .sirketId(1L)
                .olusturmaTarihi(LocalDateTime.now())
                .build();
    }

    @Test
    void getir_returnsById() {
        when(maasBordroRepository.findById(1L)).thenReturn(Optional.of(createBordro(1L)));
        var result = maasBordroService.getir(1L);
        assertEquals(1L, result.getId());
        assertEquals("Ahmet Yilmaz", result.getPersonelAdi());
        assertEquals(0, new BigDecimal("30000.00").compareTo(result.getBrutMaas()));
    }

    @Test
    void getir_notFound_throws() {
        when(maasBordroRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> maasBordroService.getir(99L));
    }

    @Test
    void olustur_creates() {
        MaasBordroDTO dto = MaasBordroDTO.builder()
                .personelId(1L).yil(2026).ay(7)
                .brutMaas(new BigDecimal("30000.00"))
                .kesintiler(new BigDecimal("9000.00"))
                .netMaas(new BigDecimal("21000.00"))
                .odemeTarihi(LocalDate.of(2026, 7, 30))
                .build();
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel()));
        when(maasBordroRepository.save(any(MaasBordro.class))).thenReturn(createBordro(1L));
        var result = maasBordroService.olustur(dto, 1L);
        assertEquals(1L, result.getPersonelId());
        assertEquals(2026, result.getYil());
    }

    @Test
    void sil_deletes() {
        when(maasBordroRepository.existsById(1L)).thenReturn(true);
        maasBordroService.sil(1L);
        verify(maasBordroRepository).deleteById(1L);
    }
}
