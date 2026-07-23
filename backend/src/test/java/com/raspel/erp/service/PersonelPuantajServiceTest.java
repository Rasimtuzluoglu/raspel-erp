package com.raspel.erp.service;

import com.raspel.erp.dto.PersonelPuantajDTO;
import com.raspel.erp.entity.Personel;
import com.raspel.erp.entity.PersonelPuantaj;
import com.raspel.erp.repository.PersonelPuantajRepository;
import com.raspel.erp.repository.PersonelRepository;
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
class PersonelPuantajServiceTest {

    @Mock private PersonelPuantajRepository puantajRepository;
    @Mock private PersonelRepository personelRepository;
    @InjectMocks private PersonelPuantajService personelPuantajService;

    private Personel createPersonel() {
        Personel p = new Personel();
        p.setId(1L);
        p.setAd("Ahmet");
        p.setSoyad("Yilmaz");
        return p;
    }

    private PersonelPuantaj createPuantaj(Long id) {
        PersonelPuantaj p = new PersonelPuantaj();
        p.setId(id);
        p.setPersonelId(1L);
        p.setTarih(LocalDate.now());
        p.setDurum("VAR");
        p.setOlusturmaTarihi(LocalDateTime.now());
        return p;
    }

    @Test
    void personelPuantajlari_returnsForPersonel() {
        when(puantajRepository.findByPersonelIdAndTarihBetweenOrderByTarihAsc(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(createPuantaj(1L)));
        var result = personelPuantajService.personelPuantajlari(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(1, result.size());
    }

    @Test
    void olustur_creates() {
        PersonelPuantajDTO dto = PersonelPuantajDTO.builder().personelId(1L)
                .tarih(LocalDate.now()).durum("IZIN").aciklama("Hastalik").build();
        PersonelPuantaj saved = createPuantaj(1L);
        saved.setDurum("IZIN");
        when(puantajRepository.save(any(PersonelPuantaj.class))).thenReturn(saved);
        var result = personelPuantajService.olustur(dto);
        assertEquals("IZIN", result.getDurum());
    }

    @Test
    void sil_deletes() {
        personelPuantajService.sil(1L);
        verify(puantajRepository).deleteById(1L);
    }
}
