package com.raspel.erp.service;

import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.ik.Vardiya;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.ik.VardiyaRepository;
import com.raspel.erp.service.ik.VardiyaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VardiyaServiceTest {

    @Mock private VardiyaRepository vardiyaRepository;
    @Mock private PersonelRepository personelRepository;
    @InjectMocks private VardiyaService vardiyaService;

    private Vardiya createVardiya(Long id, Long personelId) {
        Personel p = new Personel();
        p.setId(personelId);
        p.setAd("Ahmet");
        p.setSoyad("Yilmaz");
        return Vardiya.builder()
                .id(id)
                .personel(p)
                .tarih(LocalDate.now())
                .baslangic(LocalTime.of(8, 0))
                .bitis(LocalTime.of(17, 0))
                .tur("SABAH")
                .sirketId(1L)
                .olusturmaTarihi(LocalDateTime.now())
                .build();
    }

    @Test
    void personelVardiyalari_returnsForPersonel() {
        when(vardiyaRepository.findByPersonelIdOrderByTarihDesc(1L))
                .thenReturn(List.of(createVardiya(1L, 1L)));
        var result = vardiyaService.personelVardiyalari(1L);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getPersonelId());
        assertEquals("Ahmet Yilmaz", result.get(0).getPersonelAdi());
    }

    @Test
    void getir_returnsById() {
        when(vardiyaRepository.findById(1L)).thenReturn(Optional.of(createVardiya(1L, 1L)));
        var result = vardiyaService.getir(1L);
        assertEquals(1L, result.getId());
    }
}
