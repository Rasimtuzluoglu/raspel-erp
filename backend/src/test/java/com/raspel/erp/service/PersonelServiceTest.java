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
    @Mock private com.raspel.erp.repository.ik.PersonelIzinRepository personelIzinRepository;
    @Mock private com.raspel.erp.repository.ik.PersonelPuantajRepository personelPuantajRepository;
    @Mock private TenantChecker tenantChecker;
    @Mock private com.raspel.erp.config.CacheYardimci cacheYardimci;
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
        PersonelDTO dto = PersonelDTO.builder().ad("Mehmet").soyad("Demir").tcKimlik("10000000146")
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

    @Test
    void ozet_returnsCounts() {
        when(personelRepository.countBySirketId(1L)).thenReturn(12L);
        when(personelRepository.countByAktifTrueAndSirketId(1L)).thenReturn(9L);
        when(personelRepository.countBySirketIdAndAktifFalse(1L)).thenReturn(3L);
        when(personelIzinRepository.countByDurumAndSirketId("BEKLEMEDE", 1L)).thenReturn(2L);

        var ozet = personelService.ozet(1L);

        assertEquals(12L, ozet.getToplam());
        assertEquals(9L, ozet.getAktif());
        assertEquals(3L, ozet.getPasif());
        assertEquals(2L, ozet.getBekleyenIzin());
    }

    @Test
    void ozet_sirketYoksaSifir() {
        var ozet = personelService.ozet(null);
        assertEquals(0L, ozet.getToplam());
        assertEquals(0L, ozet.getAktif());
        assertEquals(0L, ozet.getPasif());
        assertEquals(0L, ozet.getBekleyenIzin());
        verifyNoInteractions(personelRepository, personelIzinRepository);
    }

    @Test
    void filtreli_sirketYoksaBosSayfa() {
        var sonuc = personelService.filtreli(null, "ahmet", "IT", "SOFOR", "AKTIF", Pageable.unpaged());
        assertTrue(sonuc.isEmpty());
        verifyNoInteractions(personelRepository);
    }

    @Test
    void filtreli_specIleSayfalar() {
        when(personelRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createPersonel(1L))));
        var sonuc = personelService.filtreli(1L, "  ahmet  ", "IT", "sofor", "AKTIF", Pageable.unpaged());
        assertEquals(1, sonuc.getContent().size());
        assertEquals("Ahmet", sonuc.getContent().get(0).getAd());
        verify(personelRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
    }

    @Test
    void filtreli_filtresizDeCalisir() {
        when(personelRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createPersonel(1L), createPersonel(2L))));
        var sonuc = personelService.filtreli(1L, null, null, null, null, Pageable.unpaged());
        assertEquals(2, sonuc.getContent().size());
    }

    @Test
    void departmanlar_listeler() {
        when(personelRepository.findDistinctDepartmanBySirketId(1L)).thenReturn(List.of("IT", "Muhasebe"));
        assertEquals(2, personelService.departmanlar(1L).size());
        assertTrue(personelService.departmanlar(null).isEmpty());
        verify(personelRepository, never()).findDistinctDepartmanBySirketId(null);
    }

    @Test
    void olustur_adSoyadZorunlu() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> personelService.olustur(PersonelDTO.builder().ad(" ").soyad("Demir").sirketId(1L).build()));
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> personelService.olustur(PersonelDTO.builder().ad("Mehmet").soyad(null).sirketId(1L).build()));
        verify(personelRepository, never()).save(any(Personel.class));
    }

    @Test
    void olustur_gecersizTcReddedilir() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> personelService.olustur(PersonelDTO.builder().ad("Mehmet").soyad("Demir")
                        .tcKimlik("12345678901").sirketId(1L).build()));
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> personelService.olustur(PersonelDTO.builder().ad("Mehmet").soyad("Demir")
                        .tcKimlik("123").sirketId(1L).build()));
    }

    @Test
    void olustur_ayniTcReddedilir() {
        when(personelRepository.existsByTcKimlik("10000000146")).thenReturn(true);
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> personelService.olustur(PersonelDTO.builder().ad("Mehmet").soyad("Demir")
                        .tcKimlik("10000000146").sirketId(1L).build()));
        verify(personelRepository, never()).save(any(Personel.class));
    }

    @Test
    void olustur_telefonVeEmailDogrulanir() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> personelService.olustur(PersonelDTO.builder().ad("Mehmet").soyad("Demir")
                        .telefon("abc").sirketId(1L).build()));
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> personelService.olustur(PersonelDTO.builder().ad("Mehmet").soyad("Demir")
                        .email("gecersiz").sirketId(1L).build()));
    }

    @Test
    void olustur_negatifMaasReddedilir() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> personelService.olustur(PersonelDTO.builder().ad("Mehmet").soyad("Demir")
                        .maas(BigDecimal.valueOf(-1)).sirketId(1L).build()));
    }

    @Test
    void olustur_cikisGiristenOnceOlamaz() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> personelService.olustur(PersonelDTO.builder().ad("Mehmet").soyad("Demir")
                        .iseGirisTarihi(LocalDate.of(2024, 5, 1))
                        .cikisTarihi(LocalDate.of(2024, 4, 1)).sirketId(1L).build()));
    }

    @Test
    void olustur_cikisVarsaPasif() {
        when(personelRepository.save(any(Personel.class))).thenAnswer(inv -> {
            Personel p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        var sonuc = personelService.olustur(PersonelDTO.builder().ad("Mehmet").soyad("Demir")
                .iseGirisTarihi(LocalDate.of(2024, 1, 1)).cikisTarihi(LocalDate.of(2024, 6, 1))
                .sirketId(1L).build());
        assertFalse(sonuc.getAktif());
    }

    @Test
    void guncelle_ayniTcBaskaKayitReddedilir() {
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel(1L)));
        when(personelRepository.existsByTcKimlikAndIdNot("10000000146", 1L)).thenReturn(true);
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> personelService.guncelle(1L, PersonelDTO.builder().tcKimlik("10000000146").build()));
        verify(personelRepository, never()).save(any(Personel.class));
    }

    @Test
    void detay_ozetVeSonIzinleriDoner() {
        Personel p = createPersonel(1L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(p));
        when(personelIzinRepository.findByPersonelIdOrderByBaslangicDesc(1L)).thenReturn(List.of(
                com.raspel.erp.entity.ik.PersonelIzin.builder().id(3L).personelId(1L)
                        .izinTuru("YILLIK_IZIN").baslangic(LocalDate.now().minusDays(1))
                        .bitis(LocalDate.now().plusDays(1)).gunSayisi(3).durum("ONAYLANDI").build(),
                com.raspel.erp.entity.ik.PersonelIzin.builder().id(4L).personelId(1L)
                        .izinTuru("HASTA_IZNI").baslangic(LocalDate.now()).bitis(LocalDate.now())
                        .gunSayisi(1).durum("BEKLEMEDE").build()));
        when(personelPuantajRepository.findBySirketIdAndPersonelIdAndTarihBetweenOrderByTarihAsc(
                eq(1L), eq(1L), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(
                com.raspel.erp.entity.ik.PersonelPuantaj.builder().id(1L).personelId(1L).sirketId(1L)
                        .tarih(LocalDate.now()).durum("GELDI").build(),
                com.raspel.erp.entity.ik.PersonelPuantaj.builder().id(2L).personelId(1L).sirketId(1L)
                        .tarih(LocalDate.now()).durum("GELMEDI").build()));

        var detay = personelService.detay(1L);

        assertEquals(2, detay.getSonIzinler().size());
        assertTrue(detay.isBugunIzinli());
        assertEquals(1L, detay.getBekleyenIzin());
        assertEquals(2L, detay.getPuantajKayitSayisi());
        assertEquals(1L, detay.getGeldi());
        assertEquals(1L, detay.getGelmedi());
        assertEquals("Ahmet", detay.getPersonel().getAd());
    }

    @Test
    void maas_yetkisizKullaniciyaMaskelenir() {
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel(1L)));
        var sonuc = personelService.getir(1L);
        assertNull(sonuc.getMaas());
    }

    @Test
    void maas_adminRolundeGorunur() {
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel(1L)));
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "admin", "n/a",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            var sonuc = personelService.getir(1L);
            assertEquals(BigDecimal.valueOf(15000), sonuc.getMaas());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}