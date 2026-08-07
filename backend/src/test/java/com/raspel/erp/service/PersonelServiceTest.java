package com.raspel.erp.service;

import com.raspel.erp.dto.ik.PersonelDTO;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.config.TenantChecker;
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
import com.raspel.erp.service.ik.PersonelService;

@ExtendWith(MockitoExtension.class)
class PersonelServiceTest {

    @Mock private PersonelRepository personelRepository;
    @Mock private TenantChecker tenantChecker;
    @InjectMocks private PersonelService personelService;

    private Personel createPersonel(Long id) {
        Personel p = new Personel();
        p.setId(id);
        p.setAd("Ahmet");
        p.setSoyad("Yilmaz");
        p.setTcKimlik("12345678901");
        p.setDepartman("IT");
        p.setPozisyon("Developer");
        p.setMaas(BigDecimal.valueOf(15000));
        p.setAktif(true);
        p.setSirketId(1L);
        p.setOlusturmaTarihi(LocalDateTime.now());
        return p;
    }

    @Test
    void tumunuGetir_returnsAll() {
        when(personelRepository.findBySirketIdOrderByAdAsc(1L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(createPersonel(1L))));
        var result = personelService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getir_returnsPersonel() {
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel(1L)));
        var result = personelService.getir(1L);
        assertEquals("Ahmet", result.getAd());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(personelRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> personelService.getir(99L));
    }

    @Test
    void olustur_creates() {
        PersonelDTO dto = PersonelDTO.builder().ad("Mehmet").soyad("Demir").tcKimlik("98765432109")
                .departman("Muhasebe").pozisyon("Uzman").maas(BigDecimal.valueOf(12000)).telefon("5551112233")
                .email("mehmet@test.com").aktif(true).sirketId(1L).build();
        Personel saved = createPersonel(1L);
        saved.setAd("Mehmet");
        saved.setSoyad("Demir");
        when(personelRepository.save(any(Personel.class))).thenReturn(saved);
        var result = personelService.olustur(dto);
        assertEquals("Mehmet", result.getAd());
    }

    @Test
    void guncelle_updates() {
        Personel existing = createPersonel(1L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(existing));
        PersonelDTO dto = PersonelDTO.builder().ad("Ali").soyad("Kara").telefon("5554443322").build();
        when(personelRepository.save(any(Personel.class))).thenReturn(existing);
        var result = personelService.guncelle(1L, dto);
        assertEquals("Ali", result.getAd());
    }

    @Test
    void guncelle_throwsWhenNotFound() {
        when(personelRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> personelService.guncelle(99L, new PersonelDTO()));
    }

    @Test
    void sil_deletes() {
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel(1L)));
        personelService.sil(1L);
        verify(personelRepository).deleteById(1L);
    }

    @Test
    void sil_throwsWhenNotFound() {
        when(personelRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> personelService.sil(99L));
    }
}