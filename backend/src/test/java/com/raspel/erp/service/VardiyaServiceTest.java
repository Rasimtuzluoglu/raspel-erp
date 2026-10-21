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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VardiyaServiceTest {

    @Mock private VardiyaRepository vardiyaRepository;
    @Mock private PersonelRepository personelRepository;
    @Mock private com.raspel.erp.config.TenantChecker tenantChecker;
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
        Personel p = new Personel();
        p.setId(1L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(p));
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

    @Test
    void ozet_sayilariDoner() {
        when(vardiyaRepository.countBySirketId(1L)).thenReturn(30L);
        when(vardiyaRepository.countBySirketIdAndTarih(eq(1L), any(LocalDate.class))).thenReturn(4L);
        when(vardiyaRepository.countBySirketIdAndTarihBetween(eq(1L), any(LocalDate.class), any(LocalDate.class))).thenReturn(11L);
        when(vardiyaRepository.countDistinctPersonelBySirketId(1L)).thenReturn(7L);

        var ozet = vardiyaService.ozet(1L);

        assertEquals(30L, ozet.getToplam());
        assertEquals(4L, ozet.getBugun());
        assertEquals(11L, ozet.getBuHafta());
        assertEquals(7L, ozet.getPersonelSayisi());
    }

    @Test
    void ozet_sirketYoksaSifir() {
        var ozet = vardiyaService.ozet(null);
        assertEquals(0L, ozet.getToplam());
        verifyNoInteractions(vardiyaRepository);
    }

    @Test
    void filtreli_sirketYoksaBos() {
        var sonuc = vardiyaService.filtreli(null, 1L, null, null, "SABAH", "abc",
                org.springframework.data.domain.Pageable.unpaged());
        assertTrue(sonuc.isEmpty());
        verifyNoInteractions(vardiyaRepository);
    }

    @Test
    void filtreli_sayfalar() {
        when(vardiyaRepository.filtrele(eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(createVardiya(1L, 1L))));

        var sonuc = vardiyaService.filtreli(1L, null, null, null, "TUMU", "   ",
                org.springframework.data.domain.Pageable.unpaged());

        assertEquals(1, sonuc.getContent().size());
        assertEquals("Ahmet Yilmaz", sonuc.getContent().get(0).getPersonelAdi());
    }

    @Test
    void filtreli_turVeAramaNormallestirilir() {
        when(vardiyaRepository.filtrele(eq(1L), eq(2L), isNull(), isNull(), eq("GECE"), eq("ahmet"),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        vardiyaService.filtreli(1L, 2L, null, null, "gece", "  AHMET  ",
                org.springframework.data.domain.Pageable.unpaged());

        verify(vardiyaRepository).filtrele(eq(1L), eq(2L), isNull(), isNull(), eq("GECE"), eq("ahmet"),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void olustur_ciftKayitReddedilir() {
        Personel p = new Personel();
        p.setId(1L);
        p.setSirketId(1L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(p));
        when(vardiyaRepository.existsByPersonelIdAndTarih(1L, LocalDate.of(2026, 4, 1))).thenReturn(true);

        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> vardiyaService.olustur(
                com.raspel.erp.dto.ik.VardiyaDTO.builder().personelId(1L).tarih(LocalDate.of(2026, 4, 1))
                        .baslangic(LocalTime.of(8, 0)).bitis(LocalTime.of(16, 0)).tur("SABAH").build(), 1L));
        verify(vardiyaRepository, never()).save(any(Vardiya.class));
    }

    @Test
    void olustur_gecersizTurReddedilir() {
        Personel p = new Personel();
        p.setId(1L);
        p.setSirketId(1L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> vardiyaService.olustur(
                com.raspel.erp.dto.ik.VardiyaDTO.builder().personelId(1L).tarih(LocalDate.now())
                        .baslangic(LocalTime.of(8, 0)).bitis(LocalTime.of(16, 0)).tur("BILINMEYEN").build(), 1L));
        verify(vardiyaRepository, never()).save(any(Vardiya.class));
    }

    @Test
    void olustur_ayniSaatReddedilir() {
        Personel p = new Personel();
        p.setId(1L);
        p.setSirketId(1L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> vardiyaService.olustur(
                com.raspel.erp.dto.ik.VardiyaDTO.builder().personelId(1L).tarih(LocalDate.now())
                        .baslangic(LocalTime.of(8, 0)).bitis(LocalTime.of(8, 0)).tur("SABAH").build(), 1L));
        verify(vardiyaRepository, never()).save(any(Vardiya.class));
    }

    @Test
    void olustur_tarihZorunlu() {
        Personel p = new Personel();
        p.setId(1L);
        p.setSirketId(1L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> vardiyaService.olustur(
                com.raspel.erp.dto.ik.VardiyaDTO.builder().personelId(1L)
                        .baslangic(LocalTime.of(8, 0)).bitis(LocalTime.of(16, 0)).tur("SABAH").build(), 1L));
        verify(vardiyaRepository, never()).save(any(Vardiya.class));
    }

    @Test
    void guncelle_ciftKayitReddedilir() {
        Vardiya mevcut = createVardiya(5L, 1L);
        when(vardiyaRepository.findById(5L)).thenReturn(Optional.of(mevcut));
        when(vardiyaRepository.existsByPersonelIdAndTarihAndIdNot(1L, LocalDate.of(2026, 5, 1), 5L)).thenReturn(true);

        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> vardiyaService.guncelle(5L,
                com.raspel.erp.dto.ik.VardiyaDTO.builder().tarih(LocalDate.of(2026, 5, 1)).build()));
        verify(vardiyaRepository, never()).save(any(Vardiya.class));
    }

    @Test
    void guncelle_kendiKaydiKabulEdilir() {
        Vardiya mevcut = createVardiya(5L, 1L);
        when(vardiyaRepository.findById(5L)).thenReturn(Optional.of(mevcut));
        when(vardiyaRepository.save(any(Vardiya.class))).thenAnswer(inv -> inv.getArgument(0));

        var sonuc = vardiyaService.guncelle(5L,
                com.raspel.erp.dto.ik.VardiyaDTO.builder().tur("GECE").build());

        assertEquals("GECE", sonuc.getTur());
    }
}
