package com.raspel.erp.service;

import com.raspel.erp.dto.finans.CariHesapDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.finans.CariHesapService;

@ExtendWith(MockitoExtension.class)
class CariHesapServiceTest {

    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private HareketRepository hareketRepository;
    @InjectMocks private CariHesapService cariHesapService;

    private CariHesap createCariHesap(Long id) {
        CariHesap c = new CariHesap();
        c.setId(id);
        c.setAd("Test Cari");
        c.setVergiNumarasi("1234567890");
        c.setTelefon("5551234567");
        c.setBakiye(BigDecimal.ZERO);
        c.setOlusturmaTarihi(LocalDateTime.now());
        c.setGuncellemeTarihi(LocalDateTime.now());
        return c;
    }

    @Test
    void tumCariHesaplariGetir_returnsAll() {
        when(cariHesapRepository.findBySirketId(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(createCariHesap(1L), createCariHesap(2L))));
        var result = cariHesapService.tumCariHesaplariGetir(1L, Pageable.unpaged());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void cariHesapAra_returnsFiltered() {
        when(cariHesapRepository.findBySirketIdAndAdContainingIgnoreCase(anyLong(), eq("test"))).thenReturn(List.of(createCariHesap(1L)));
        var result = cariHesapService.cariHesapAra("test", 1L);
        assertEquals(1, result.size());
    }

    @Test
    void cariHesapGetir_returnsCariHesap() {
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(createCariHesap(1L)));
        var result = cariHesapService.cariHesapGetir(1L);
        assertEquals("Test Cari", result.getAd());
    }

    @Test
    void cariHesapGetir_throwsWhenNotFound() {
        when(cariHesapRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> cariHesapService.cariHesapGetir(99L));
    }

    @Test
    void cariHesapOlustur_creates() {
        CariHesapDTO dto = CariHesapDTO.builder().ad("Yeni Cari").vergiNumarasi("9876543210").telefon("5559876543").build();
        CariHesap saved = createCariHesap(1L);
        saved.setAd("Yeni Cari");
        when(cariHesapRepository.save(any(CariHesap.class))).thenReturn(saved);
        var result = cariHesapService.cariHesapOlustur(dto, 1L);
        assertEquals("Yeni Cari", result.getAd());
    }

    @Test
    void cariHesapGuncelle_updates() {
        CariHesap existing = createCariHesap(1L);
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(existing));
        CariHesapDTO dto = CariHesapDTO.builder().ad("Guncel Cari").vergiNumarasi("111").telefon("222").build();
        when(cariHesapRepository.save(any(CariHesap.class))).thenReturn(existing);
        var result = cariHesapService.cariHesapGuncelle(1L, dto);
        assertEquals("Guncel Cari", result.getAd());
    }

    @Test
    void cariHesapGuncelle_throwsWhenNotFound() {
        when(cariHesapRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> cariHesapService.cariHesapGuncelle(99L, new CariHesapDTO()));
    }

    @Test
    void cariHesapSil_deletes() {
        when(cariHesapRepository.existsById(1L)).thenReturn(true);
        when(hareketRepository.countByCariHesapId(1L)).thenReturn(0L);
        cariHesapService.cariHesapSil(1L);
        verify(cariHesapRepository).deleteById(1L);
    }

    @Test
    void cariHesapSil_throwsWhenHasHareket() {
        when(cariHesapRepository.existsById(1L)).thenReturn(true);
        when(hareketRepository.countByCariHesapId(1L)).thenReturn(3L);
        assertThrows(RuntimeException.class, () -> cariHesapService.cariHesapSil(1L));
    }

    @Test
    void cariHesapSil_throwsWhenNotFound() {
        when(cariHesapRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> cariHesapService.cariHesapSil(99L));
    }

    @Test
    void bakiyeGuncelle_updatesBalance() {
        CariHesap cari = createCariHesap(1L);
        cari.setBakiye(BigDecimal.valueOf(100));
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));
        cariHesapService.bakiyeGuncelle(1L, BigDecimal.valueOf(50));
        assertEquals(BigDecimal.valueOf(150), cari.getBakiye());
        verify(cariHesapRepository).save(cari);
    }

    @Test
    void toplamCariSayisiGetir_returnsCount() {
        when(cariHesapRepository.count()).thenReturn(5L);
        assertEquals(5L, cariHesapService.toplamCariSayisiGetir());
    }

    @Test
    void toplamBakiyeGetir_returnsTotal() {
        when(cariHesapRepository.toplamBakiyeHesapla()).thenReturn(BigDecimal.valueOf(50000));
        assertEquals(BigDecimal.valueOf(50000), cariHesapService.toplamBakiyeGetir());
    }
}