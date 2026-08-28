package com.raspel.erp.service;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ik.PersonelPuantajDTO;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.ik.PersonelPuantaj;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ik.PersonelPuantajRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.ik.PersonelPuantajService;

@ExtendWith(MockitoExtension.class)
class PersonelPuantajServiceTest {

    @Mock private PersonelPuantajRepository puantajRepository;
    @Mock private PersonelRepository personelRepository;
    @Spy private TenantChecker tenantChecker = new TenantChecker();
    @InjectMocks private PersonelPuantajService personelPuantajService;

    @AfterEach
    void temizle() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void oturumSirket(Long sirketId) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute("sirketId", sirketId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    private Personel createPersonel(Long sirketId) {
        Personel p = new Personel();
        p.setId(1L);
        p.setAd("Ahmet");
        p.setSoyad("Yilmaz");
        p.setSirketId(sirketId);
        return p;
    }

    private PersonelPuantaj createPuantaj(Long id, Long sirketId) {
        PersonelPuantaj p = new PersonelPuantaj();
        p.setId(id);
        p.setPersonelId(1L);
        p.setSirketId(sirketId);
        p.setTarih(LocalDate.now());
        p.setDurum("VAR");
        p.setOlusturmaTarihi(LocalDateTime.now());
        return p;
    }

    @Test
    void personelPuantajlari_returnsForPersonel() {
        oturumSirket(1L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel(1L)));
        when(puantajRepository.findBySirketIdAndPersonelIdAndTarihBetweenOrderByTarihAsc(1L, 1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(createPuantaj(1L, 1L)));
        var result = personelPuantajService.personelPuantajlari(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(1, result.size());
    }

    @Test
    void personelPuantajlari_baskaSirketPersoneli_IzinVerilmez() {
        oturumSirket(2L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel(1L)));
        assertThrows(ResourceNotFoundException.class,
                () -> personelPuantajService.personelPuantajlari(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
        verify(puantajRepository, never()).findBySirketIdAndPersonelIdAndTarihBetweenOrderByTarihAsc(anyLong(), anyLong(), any(), any());
    }

    @Test
    void olustur_creates() {
        oturumSirket(1L);
        PersonelPuantajDTO dto = PersonelPuantajDTO.builder().personelId(1L)
                .tarih(LocalDate.now()).durum("IZIN").aciklama("Hastalik").build();
        PersonelPuantaj saved = createPuantaj(1L, 1L);
        saved.setDurum("IZIN");
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel(1L)));
        when(puantajRepository.save(any(PersonelPuantaj.class))).thenReturn(saved);
        var result = personelPuantajService.olustur(dto);
        assertEquals("IZIN", result.getDurum());
        assertEquals(1L, result.getSirketId());
    }

    @Test
    void olustur_baskaSirketPersoneline_KayitOlusturulamaz() {
        oturumSirket(2L);
        PersonelPuantajDTO dto = PersonelPuantajDTO.builder().personelId(1L)
                .tarih(LocalDate.now()).durum("VAR").build();
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel(1L)));
        assertThrows(ResourceNotFoundException.class, () -> personelPuantajService.olustur(dto));
        verify(puantajRepository, never()).save(any());
    }

    @Test
    void guncelle_baskaSirketKaydiniDegistiremez() {
        oturumSirket(2L);
        when(puantajRepository.findById(5L)).thenReturn(Optional.of(createPuantaj(5L, 1L)));
        PersonelPuantajDTO dto = PersonelPuantajDTO.builder().durum("VAR").build();
        assertThrows(ResourceNotFoundException.class, () -> personelPuantajService.guncelle(5L, dto));
    }

    @Test
    void sil_deletes() {
        oturumSirket(1L);
        when(puantajRepository.findById(1L)).thenReturn(Optional.of(createPuantaj(1L, 1L)));
        personelPuantajService.sil(1L);
        verify(puantajRepository).deleteById(1L);
    }

    @Test
    void sil_baskaSirketKaydiniSilemez() {
        oturumSirket(2L);
        when(puantajRepository.findById(1L)).thenReturn(Optional.of(createPuantaj(1L, 1L)));
        assertThrows(ResourceNotFoundException.class, () -> personelPuantajService.sil(1L));
        verify(puantajRepository, never()).deleteById(anyLong());
    }
}
