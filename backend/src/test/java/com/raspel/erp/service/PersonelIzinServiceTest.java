package com.raspel.erp.service;

import com.raspel.erp.dto.PersonelIzinDTO;
import com.raspel.erp.entity.PersonelIzin;
import com.raspel.erp.repository.PersonelIzinRepository;
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
class PersonelIzinServiceTest {

    @Mock private PersonelIzinRepository izinRepository;
    @Mock private PersonelRepository personelRepository;
    @InjectMocks private PersonelIzinService personelIzinService;

    private PersonelIzin createIzin(Long id) {
        PersonelIzin i = new PersonelIzin();
        i.setId(id);
        i.setPersonelId(1L);
        i.setIzinTuru("YILLIK");
        i.setBaslangic(LocalDate.now());
        i.setBitis(LocalDate.now().plusDays(5));
        i.setGunSayisi(5);
        i.setDurum("BEKLEMEDE");
        i.setOlusturmaTarihi(LocalDateTime.now());
        return i;
    }

    @Test
    void tumunuGetir_returnsAll() {
        when(izinRepository.findAll()).thenReturn(List.of(createIzin(1L), createIzin(2L)));
        var result = personelIzinService.tumunuGetir();
        assertEquals(2, result.size());
    }

    @Test
    void personelIzınleri_returnsForPersonel() {
        when(izinRepository.findByPersonelIdOrderByBaslangicDesc(1L)).thenReturn(List.of(createIzin(1L)));
        var result = personelIzinService.personelIzınleri(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getir_returnsIzin() {
        when(izinRepository.findById(1L)).thenReturn(Optional.of(createIzin(1L)));
        var result = personelIzinService.getir(1L);
        assertEquals("YILLIK", result.getIzinTuru());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(izinRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> personelIzinService.getir(99L));
    }

    @Test
    void olustur_creates() {
        PersonelIzinDTO dto = PersonelIzinDTO.builder().personelId(1L).izinTuru("HASTALIK")
                .baslangic(LocalDate.now()).bitis(LocalDate.now().plusDays(3)).gunSayisi(3).build();
        PersonelIzin saved = createIzin(1L);
        saved.setIzinTuru("HASTALIK");
        when(izinRepository.save(any(PersonelIzin.class))).thenReturn(saved);
        var result = personelIzinService.olustur(dto);
        assertEquals("HASTALIK", result.getIzinTuru());
        assertEquals("BEKLEMEDE", result.getDurum());
    }

    @Test
    void durumGuncelle_updates() {
        PersonelIzin izin = createIzin(1L);
        when(izinRepository.findById(1L)).thenReturn(Optional.of(izin));
        when(izinRepository.save(any(PersonelIzin.class))).thenReturn(izin);
        var result = personelIzinService.durumGuncelle(1L, "ONAYLANDI", "Yonetici");
        assertEquals("ONAYLANDI", result.getDurum());
        assertEquals("Yonetici", result.getOnaylayan());
    }

    @Test
    void durumGuncelle_throwsWhenNotFound() {
        when(izinRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> personelIzinService.durumGuncelle(99L, "ONAYLANDI", "Yonetici"));
    }

    @Test
    void sil_deletes() {
        personelIzinService.sil(1L);
        verify(izinRepository).deleteById(1L);
    }
}
