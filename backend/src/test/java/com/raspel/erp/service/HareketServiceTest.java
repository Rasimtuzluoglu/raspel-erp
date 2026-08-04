package com.raspel.erp.service;

import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.sistem.BildirimService;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.service.finans.HareketService;

@ExtendWith(MockitoExtension.class)
class HareketServiceTest {

    @Mock private HareketRepository hareketRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private CariHesapService cariHesapService;
    @Mock private BildirimService bildirimService;
    @InjectMocks private HareketService hareketService;

    private CariHesap createCariHesap() {
        CariHesap c = new CariHesap();
        c.setId(1L);
        c.setAd("Test Cari");
        c.setBakiye(BigDecimal.valueOf(1000));
        c.setOlusturmaTarihi(LocalDateTime.now());
        c.setGuncellemeTarihi(LocalDateTime.now());
        return c;
    }

    private Hareket createHareket(Long id) {
        CariHesap cari = createCariHesap();
        Hareket h = new Hareket();
        h.setId(id);
        h.setCariHesap(cari);
        h.setTur(Hareket.HareketTuru.TAHSILAT);
        h.setTutar(BigDecimal.valueOf(500));
        h.setHareketTarihi(LocalDate.now());
        h.setOlusturmaTarihi(LocalDateTime.now());
        return h;
    }

    @Test
    void cariHesapHareketleriGetir_returnsHareketler() {
        when(cariHesapRepository.existsById(1L)).thenReturn(true);
        when(hareketRepository.findByCariHesapIdOrderByHareketTarihiDesc(1L)).thenReturn(List.of(createHareket(1L)));
        var result = hareketService.cariHesapHareketleriGetir(1L);
        assertEquals(1, result.size());
    }

    @Test
    void cariHesapHareketleriGetir_throwsWhenCariNotFound() {
        when(cariHesapRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> hareketService.cariHesapHareketleriGetir(99L));
    }

    @Test
    void sonHareketleriGetir_returnsRecent() {
        when(hareketRepository.findAllByOrderByHareketTarihiDescOlusturmaTarihiDesc(PageRequest.of(0, 5)))
                .thenReturn(List.of(createHareket(1L)));
        var result = hareketService.sonHareketleriGetir(5);
        assertEquals(1, result.size());
    }

    @Test
    void hareketOlustur_creates() {
        CariHesap cari = createCariHesap();
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));
        HareketDTO dto = HareketDTO.builder().cariHesapId(1L).tur("TAHSILAT")
                .tutar(BigDecimal.valueOf(1000)).hareketTarihi(LocalDate.now()).build();
        Hareket saved = createHareket(1L);
        saved.setTutar(BigDecimal.valueOf(1000));
        when(hareketRepository.save(any(Hareket.class))).thenReturn(saved);
        doNothing().when(cariHesapService).bakiyeGuncelle(1L, BigDecimal.valueOf(1000));
        var result = hareketService.hareketOlustur(dto, 1L);
        assertEquals(Hareket.HareketTuru.TAHSILAT.name(), result.getTur());
    }

    @Test
    void hareketOlustur_throwsWhenInvalidTur() {
        HareketDTO dto = HareketDTO.builder().cariHesapId(1L).tur("INVALID").tutar(BigDecimal.valueOf(100)).build();
        assertThrows(RuntimeException.class, () -> hareketService.hareketOlustur(dto, 1L));
    }

    @Test
    void hareketOlustur_throwsWhenCariNotFound() {
        when(cariHesapRepository.findById(99L)).thenReturn(Optional.empty());
        HareketDTO dto = HareketDTO.builder().cariHesapId(99L).tur("TAHSILAT")
                .tutar(BigDecimal.valueOf(100)).build();
        assertThrows(RuntimeException.class, () -> hareketService.hareketOlustur(dto, 1L));
    }

    @Test
    void tumHareketleriGetir_returnsAll() {
        when(hareketRepository.findBySirketIdOrderByHareketTarihiDesc(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(createHareket(1L))));
        var result = hareketService.tumHareketleriGetir(1L, Pageable.unpaged());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void hareketleriFiltrele_withCari() {
        LocalDate bas = LocalDate.of(2026, 1, 1);
        LocalDate bit = LocalDate.of(2026, 12, 31);
        when(hareketRepository.findByCariHesapIdAndHareketTarihiBetweenOrderByHareketTarihiDesc(1L, bas, bit))
                .thenReturn(List.of(createHareket(1L)));
        var result = hareketService.hareketleriFiltrele(1L, bas, bit);
        assertEquals(1, result.size());
    }

    @Test
    void hareketleriFiltrele_withoutCari() {
        LocalDate bas = LocalDate.of(2026, 1, 1);
        when(hareketRepository.findByHareketTarihiBetweenOrderByHareketTarihiDesc(bas, LocalDate.now().plusDays(1)))
                .thenReturn(List.of(createHareket(1L)));
        var result = hareketService.hareketleriFiltrele(null, bas, null);
        assertEquals(1, result.size());
    }

    @Test
    void hareketGuncelle_updates() {
        Hareket existing = createHareket(1L);
        existing.setTur(Hareket.HareketTuru.ODEME);
        existing.setTutar(BigDecimal.valueOf(300));
        CariHesap cari = createCariHesap();
        when(hareketRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));
        HareketDTO dto = HareketDTO.builder().cariHesapId(1L).tur("TAHSILAT")
                .tutar(BigDecimal.valueOf(500)).hareketTarihi(LocalDate.now()).build();
        when(hareketRepository.save(any(Hareket.class))).thenReturn(existing);
        doNothing().when(cariHesapService).bakiyeGuncelle(1L, BigDecimal.valueOf(800));
        var result = hareketService.hareketGuncelle(1L, dto);
        assertNotNull(result);
    }

    @Test
    void hareketGuncelle_throwsWhenNotFound() {
        when(hareketRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> hareketService.hareketGuncelle(99L, new HareketDTO()));
    }

    @Test
    void hareketSil_deletes() {
        Hareket hareket = createHareket(1L);
        when(hareketRepository.findById(1L)).thenReturn(Optional.of(hareket));
        hareketService.hareketSil(1L);
        verify(hareketRepository).deleteById(1L);
    }

    @Test
    void hareketSil_throwsWhenNotFound() {
        when(hareketRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> hareketService.hareketSil(99L));
    }
}