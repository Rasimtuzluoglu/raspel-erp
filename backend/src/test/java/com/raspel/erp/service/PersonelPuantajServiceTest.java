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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
                .tarih(LocalDate.now()).durum("IZINLI").aciklama("Hastalik").build();
        PersonelPuantaj saved = createPuantaj(1L, 1L);
        saved.setDurum("IZINLI");
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel(1L)));
        when(puantajRepository.save(any(PersonelPuantaj.class))).thenReturn(saved);
        var result = personelPuantajService.olustur(dto);
        assertEquals("IZINLI", result.getDurum());
        assertEquals(1L, result.getSirketId());
    }

    @Test
    void olustur_baskaSirketPersoneline_KayitOlusturulamaz() {
        oturumSirket(2L);
        PersonelPuantajDTO dto = PersonelPuantajDTO.builder().personelId(1L)
                .tarih(LocalDate.now()).durum("GELDI").build();
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel(1L)));
        assertThrows(ResourceNotFoundException.class, () -> personelPuantajService.olustur(dto));
        verify(puantajRepository, never()).save(any());
    }

    @Test
    void guncelle_baskaSirketKaydiniDegistiremez() {
        oturumSirket(2L);
        when(puantajRepository.findById(5L)).thenReturn(Optional.of(createPuantaj(5L, 1L)));
        PersonelPuantajDTO dto = PersonelPuantajDTO.builder().durum("GELDI").build();
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

    @Test
    void ozet_sayilariDoner() {
        when(puantajRepository.countBySirketId(1L)).thenReturn(20L);
        when(puantajRepository.countBySirketIdAndDurum(1L, "GELDI")).thenReturn(12L);
        when(puantajRepository.countBySirketIdAndDurum(1L, "GELMEDI")).thenReturn(3L);
        when(puantajRepository.countBySirketIdAndDurum(1L, "IZINLI")).thenReturn(4L);
        when(puantajRepository.countBySirketIdAndDurum(1L, "MAZERETLI")).thenReturn(1L);

        var ozet = personelPuantajService.ozet(1L);

        assertEquals(20L, ozet.getToplam());
        assertEquals(12L, ozet.getGeldi());
        assertEquals(3L, ozet.getGelmedi());
        assertEquals(4L, ozet.getIzinli());
        assertEquals(1L, ozet.getMazeretli());
    }

    @Test
    void ozet_sirketYoksaSifir() {
        var ozet = personelPuantajService.ozet(null);
        assertEquals(0L, ozet.getToplam());
        verifyNoInteractions(puantajRepository);
    }

    @Test
    void filtreli_sirketYoksaBos() {
        var sonuc = personelPuantajService.filtreli(null, 1L, null, null, "GELDI", "abc", org.springframework.data.domain.Pageable.unpaged());
        assertTrue(sonuc.isEmpty());
        verifyNoInteractions(puantajRepository);
    }

    @Test
    void filtreli_sayfalarVeAdlariDoldurur() {
        PersonelPuantaj kayit = createPuantaj(1L, 1L);
        when(puantajRepository.filtrele(eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(kayit)));
        com.raspel.erp.entity.ik.Personel p = new com.raspel.erp.entity.ik.Personel();
        p.setId(1L);
        p.setAd("Ahmet");
        p.setSoyad("Yılmaz");
        when(personelRepository.findAllById(anyList())).thenReturn(List.of(p));

        var sonuc = personelPuantajService.filtreli(1L, null, null, null, "TUMU", "   ",
                org.springframework.data.domain.Pageable.unpaged());

        assertEquals(1, sonuc.getContent().size());
        assertEquals("Ahmet Yılmaz", sonuc.getContent().get(0).getPersonelAdi());
    }

    @Test
    void filtreli_durumVeAramaNormallestirilir() {
        when(puantajRepository.filtrele(eq(1L), eq(3L), isNull(), isNull(), eq("GELDI"), eq("ahmet"),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        personelPuantajService.filtreli(1L, 3L, null, null, "geldi", "  AHMET  ",
                org.springframework.data.domain.Pageable.unpaged());

        verify(puantajRepository).filtrele(eq(1L), eq(3L), isNull(), isNull(), eq("GELDI"), eq("ahmet"),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void olustur_ciftKayitReddedilir() {
        oturumSirket(1L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel(1L)));
        when(puantajRepository.existsByPersonelIdAndTarih(1L, LocalDate.of(2026, 4, 1))).thenReturn(true);

        assertThrows(BusinessException.class, () -> personelPuantajService.olustur(
                PersonelPuantajDTO.builder().personelId(1L).tarih(LocalDate.of(2026, 4, 1)).durum("GELDI").build()));
        verify(puantajRepository, never()).save(any(PersonelPuantaj.class));
    }

    @Test
    void olustur_gecersizDurumReddedilir() {
        oturumSirket(1L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel(1L)));

        assertThrows(BusinessException.class, () -> personelPuantajService.olustur(
                PersonelPuantajDTO.builder().personelId(1L).tarih(LocalDate.now()).durum("BILINMEYEN").build()));
        verify(puantajRepository, never()).save(any(PersonelPuantaj.class));
    }

    @Test
    void olustur_tarihZorunlu() {
        oturumSirket(1L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel(1L)));

        assertThrows(BusinessException.class, () -> personelPuantajService.olustur(
                PersonelPuantajDTO.builder().personelId(1L).durum("GELDI").build()));
        verify(puantajRepository, never()).save(any(PersonelPuantaj.class));
    }

    @Test
    void guncelle_ciftKayitReddedilir() {
        oturumSirket(1L);
        PersonelPuantaj mevcut = createPuantaj(5L, 1L);
        when(puantajRepository.findById(5L)).thenReturn(Optional.of(mevcut));
        when(puantajRepository.existsByPersonelIdAndTarihAndIdNot(1L, LocalDate.of(2026, 5, 1), 5L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> personelPuantajService.guncelle(5L,
                PersonelPuantajDTO.builder().tarih(LocalDate.of(2026, 5, 1)).durum("GELDI").build()));
        verify(puantajRepository, never()).save(any(PersonelPuantaj.class));
    }

    @Test
    void guncelle_kendiKaydiKabulEdilir() {
        oturumSirket(1L);
        PersonelPuantaj mevcut = createPuantaj(5L, 1L);
        when(puantajRepository.findById(5L)).thenReturn(Optional.of(mevcut));
        when(puantajRepository.save(any(PersonelPuantaj.class))).thenAnswer(inv -> inv.getArgument(0));

        var sonuc = personelPuantajService.guncelle(5L,
                PersonelPuantajDTO.builder().durum("GELMEDI").build());

        assertEquals("GELMEDI", sonuc.getDurum());
    }
}
