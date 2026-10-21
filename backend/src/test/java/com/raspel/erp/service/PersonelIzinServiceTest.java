package com.raspel.erp.service;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ik.PersonelIzinDTO;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.ik.PersonelIzin;
import com.raspel.erp.repository.ik.PersonelIzinRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.ik.PersonelIzinService;

@ExtendWith(MockitoExtension.class)
class PersonelIzinServiceTest {

    @Mock private PersonelIzinRepository izinRepository;
    @Mock private PersonelRepository personelRepository;
    @Mock private TenantChecker tenantChecker;
    @Mock private com.raspel.erp.config.CacheYardimci cacheYardimci;
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
        var personel = new Personel();
        personel.setId(1L);
        personel.setAd("Test");
        personel.setSoyad("User");
        when(personelRepository.findBySirketIdOrderByAdAsc(1L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(personel)));
        when(izinRepository.findByPersonelIdIn(eq(List.of(1L)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createIzin(1L), createIzin(2L))));
        var result = personelIzinService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void personelIzınleri_returnsForPersonel() {
        // C1: personel tenant'a ait olmalı (yetkisiz başka şirket personelinin
        // izinleri görüntülenemez).
        var personel = new Personel();
        personel.setId(1L);
        personel.setSirketId(1L);
        personel.setAd("Test");
        personel.setSoyad("User");
        when(personelRepository.findById(1L)).thenReturn(Optional.of(personel));
        when(izinRepository.findByPersonelIdOrderByBaslangicDesc(1L)).thenReturn(List.of(createIzin(1L)));
        var result = personelIzinService.personelIzınleri(1L);
        assertEquals(1, result.size());
        verify(tenantChecker).check(1L, "Personel Izin");
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
        Personel p = new Personel();
        p.setId(1L);
        p.setAd("Ahmet");
        p.setSoyad("Yilmaz");
        p.setSirketId(1L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(p));
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
    void olustur_gunSayisiSunucudaHesaplanir() {
        Personel p = new Personel();
        p.setId(1L);
        p.setAd("Ahmet");
        p.setSoyad("Yilmaz");
        p.setSirketId(1L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(p));
        var tutucu = org.mockito.ArgumentCaptor.forClass(PersonelIzin.class);
        when(izinRepository.save(tutucu.capture())).thenAnswer(inv -> inv.getArgument(0));

        personelIzinService.olustur(PersonelIzinDTO.builder().personelId(1L).izinTuru("YILLIK_IZIN")
                .baslangic(LocalDate.of(2026, 1, 1)).bitis(LocalDate.of(2026, 1, 5)).gunSayisi(99).build());

        assertEquals(5, tutucu.getValue().getGunSayisi());
    }

    @Test
    void olustur_izinTuruZorunlu() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> personelIzinService.olustur(PersonelIzinDTO.builder().personelId(1L)
                        .baslangic(LocalDate.now()).bitis(LocalDate.now()).build()));
        verify(izinRepository, never()).save(any(PersonelIzin.class));
    }

    @Test
    void olustur_bitisOnceGirisReddedilir() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> personelIzinService.olustur(PersonelIzinDTO.builder().personelId(1L).izinTuru("YILLIK_IZIN")
                        .baslangic(LocalDate.of(2026, 5, 10)).bitis(LocalDate.of(2026, 5, 1)).build()));
        verify(izinRepository, never()).save(any(PersonelIzin.class));
    }

    @Test
    void guncelle_tarihDegisinceGunYenilenir() {
        Personel p = new Personel();
        p.setId(1L);
        p.setAd("Ahmet");
        p.setSoyad("Yilmaz");
        p.setSirketId(1L);
        when(personelRepository.findById(1L)).thenReturn(Optional.of(p));
        PersonelIzin izin = createIzin(1L);
        when(izinRepository.findById(1L)).thenReturn(Optional.of(izin));
        when(izinRepository.save(any(PersonelIzin.class))).thenAnswer(inv -> inv.getArgument(0));

        var sonuc = personelIzinService.guncelle(1L, PersonelIzinDTO.builder()
                .baslangic(LocalDate.of(2026, 3, 1)).bitis(LocalDate.of(2026, 3, 3)).gunSayisi(1).build());

        assertEquals(3, sonuc.getGunSayisi());
        assertEquals(LocalDate.of(2026, 3, 1), sonuc.getBaslangic());
    }

    @Test
    void durumGuncelle_gecersizDurumReddedilir() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> personelIzinService.durumGuncelle(1L, "BILINMEYEN", "Yonetici"));
        verifyNoInteractions(izinRepository);
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
        when(izinRepository.findById(1L)).thenReturn(Optional.of(createIzin(1L)));
        personelIzinService.sil(1L);
        verify(izinRepository).deleteById(1L);
    }

    @Test
    void ozet_sayilariDoner() {
        when(izinRepository.countByDurumAndSirketId("BEKLEMEDE", 1L)).thenReturn(4L);
        when(izinRepository.countByDurumAndSirketId("ONAYLANDI", 1L)).thenReturn(7L);
        when(izinRepository.countByDurumAndSirketId("REDDEDILDI", 1L)).thenReturn(2L);
        when(izinRepository.countBugunIzinliAndSirketId(any(LocalDate.class), eq(1L))).thenReturn(3L);

        var ozet = personelIzinService.ozet(1L);

        assertEquals(4L, ozet.getBekleyen());
        assertEquals(7L, ozet.getOnaylandi());
        assertEquals(2L, ozet.getReddedildi());
        assertEquals(3L, ozet.getBugunIzinli());
    }

    @Test
    void ozet_sirketYoksaSifir() {
        var ozet = personelIzinService.ozet(null);
        assertEquals(0L, ozet.getBekleyen());
        assertEquals(0L, ozet.getOnaylandi());
        assertEquals(0L, ozet.getReddedildi());
        assertEquals(0L, ozet.getBugunIzinli());
        verifyNoInteractions(izinRepository);
    }

    @Test
    void filtreli_sirketYoksaBos() {
        var sonuc = personelIzinService.filtreli(null, "BEKLEMEDE", 1L, null, null, "abc", Pageable.unpaged());
        assertTrue(sonuc.isEmpty());
        verifyNoInteractions(izinRepository);
    }

    @Test
    void filtreli_sayfalarVeAdlariDoldurur() {
        when(izinRepository.filtrele(eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createIzin(1L))));
        Personel p = new Personel();
        p.setId(1L);
        p.setAd("Ahmet");
        p.setSoyad("Yılmaz");
        when(personelRepository.findAllById(anyList())).thenReturn(List.of(p));

        var sonuc = personelIzinService.filtreli(1L, "TUMU", null, null, null, "   ", Pageable.unpaged());

        assertEquals(1, sonuc.getContent().size());
        assertEquals("Ahmet Yılmaz", sonuc.getContent().get(0).getPersonelAdi());
    }

    @Test
    void filtreli_durumVeAramaNormallestirilir() {
        when(izinRepository.filtrele(eq(1L), eq("BEKLEMEDE"), eq(5L), isNull(), isNull(), eq("ahmet"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        personelIzinService.filtreli(1L, "beklemede", 5L, null, null, "  AHMET  ", Pageable.unpaged());

        verify(izinRepository).filtrele(eq(1L), eq("BEKLEMEDE"), eq(5L), isNull(), isNull(), eq("ahmet"), any(Pageable.class));
    }
}