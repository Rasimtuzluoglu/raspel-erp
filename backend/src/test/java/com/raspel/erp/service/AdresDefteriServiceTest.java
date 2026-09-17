package com.raspel.erp.service;

import com.raspel.erp.dto.sistem.AdresDefteriDTO;
import com.raspel.erp.entity.sistem.AdresDefteri;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.AdresDefteriRepository;
import com.raspel.erp.service.sistem.AdresDefteriService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdresDefteriServiceTest {

    @Mock private AdresDefteriRepository adresDefteriRepository;
    @Mock private com.raspel.erp.config.TenantChecker tenantChecker;
    @InjectMocks private AdresDefteriService service;

    private AdresDefteri kayit(Long id) {
        return AdresDefteri.builder()
                .id(id).sirketId(1L).ad("Elektrikçi Ali").tur("Elektrikçi")
                .telefon("0532 111 22 33").email("ali@example.com").adres("Ankara")
                .etiketler("acil, usta").olusturmaTarihi(LocalDateTime.now())
                .build();
    }

    @Test
    void tumunuGetir_nullSirketIdBosSayfa() {
        assertTrue(service.tumunuGetir(null, Pageable.unpaged()).isEmpty());
    }

    @Test
    void tumunuGetir_listeler() {
        when(adresDefteriRepository.findBySirketIdOrderByAdAsc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(kayit(1L), kayit(2L))));
        assertEquals(2, service.tumunuGetir(1L, Pageable.unpaged()).getTotalElements());
    }

    @Test
    void filtrele_parametreleriKullanir() {
        when(adresDefteriRepository.filtreli(eq(1L), eq("ali"), eq("Elektrikçi"), eq("acil"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(kayit(1L))));
        var sonuc = service.filtrele(1L, " ali ", "Elektrikçi", "acil", Pageable.unpaged());
        assertEquals(1, sonuc.getTotalElements());
    }

    @Test
    void filtrele_bosParametrelerNullOlur() {
        when(adresDefteriRepository.filtreli(eq(1L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        assertTrue(service.filtrele(1L, "  ", "", null, Pageable.unpaged()).isEmpty());
    }

    @Test
    void getir_tenantKontroluVeDonusum() {
        when(adresDefteriRepository.findById(1L)).thenReturn(Optional.of(kayit(1L)));
        var dto = service.getir(1L);
        assertEquals("Elektrikçi Ali", dto.getAd());
        verify(tenantChecker).check(1L, "AdresDefteri");
    }

    @Test
    void getir_bulunamazsaHata() {
        when(adresDefteriRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getir(9L));
    }

    @Test
    void olustur_alanlariKirparVeEtiketleriNormallestirir() {
        AdresDefteriDTO dto = AdresDefteriDTO.builder()
                .ad("  Tesisatçı Veli  ").tur(" Tesisatçı ").telefon(" 0212 000 00 00 ")
                .etiketler(" acil , tesisat, acil ").build();
        when(adresDefteriRepository.save(any(AdresDefteri.class))).thenAnswer(i -> i.getArgument(0));

        service.olustur(dto, 1L);

        ArgumentCaptor<AdresDefteri> captor = ArgumentCaptor.forClass(AdresDefteri.class);
        verify(adresDefteriRepository).save(captor.capture());
        AdresDefteri kaydedilen = captor.getValue();
        assertEquals("Tesisatçı Veli", kaydedilen.getAd());
        assertEquals("Tesisatçı", kaydedilen.getTur());
        assertEquals("0212 000 00 00", kaydedilen.getTelefon());
        assertEquals("acil, tesisat", kaydedilen.getEtiketler());
        assertEquals(1L, kaydedilen.getSirketId());
    }

    @Test
    void guncelle_tenantKontroluVeAlanGuncelleme() {
        when(adresDefteriRepository.findById(1L)).thenReturn(Optional.of(kayit(1L)));
        when(adresDefteriRepository.save(any(AdresDefteri.class))).thenAnswer(i -> i.getArgument(0));
        AdresDefteriDTO dto = AdresDefteriDTO.builder().ad("Ali Usta").tur("Elektrikçi")
                .telefon("0555").etiketler("usta").build();

        var sonuc = service.guncelle(1L, dto);

        assertEquals("Ali Usta", sonuc.getAd());
        assertEquals("usta", sonuc.getEtiketler());
        verify(tenantChecker).check(1L, "AdresDefteri");
    }

    @Test
    void sil_tenantKontroluVeSilme() {
        AdresDefteri a = kayit(1L);
        when(adresDefteriRepository.findById(1L)).thenReturn(Optional.of(a));
        service.sil(1L);
        verify(adresDefteriRepository).delete(a);
    }

    @Test
    void etiketListesi_ayristirirTekrarsizSiralar() {
        when(adresDefteriRepository.etiketSatirlari(1L)).thenReturn(List.of("acil, usta", "usta, tesisat"));
        var sonuc = service.etiketListesi(1L);
        assertEquals(List.of("acil", "tesisat", "usta"), sonuc);
    }

    @Test
    void turListesi_nullSirketIdBos() {
        assertTrue(service.turListesi(null).isEmpty());
    }
}
