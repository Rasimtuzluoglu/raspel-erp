package com.raspel.erp.service;

import com.raspel.erp.dto.StokDTO;
import com.raspel.erp.dto.StokHareketDTO;
import com.raspel.erp.entity.CariHesap;
import com.raspel.erp.entity.Stok;
import com.raspel.erp.entity.StokHareket;
import com.raspel.erp.repository.CariHesapRepository;
import com.raspel.erp.repository.StokHareketRepository;
import com.raspel.erp.repository.StokRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StokServiceTest {

    @Mock private StokRepository stokRepository;
    @Mock private StokHareketRepository stokHareketRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @InjectMocks private StokService stokService;

    private Stok createStok(Long id) {
        Stok s = new Stok();
        s.setId(id);
        s.setStokKodu("STK00" + id);
        s.setAd("Urun " + id);
        s.setBirim("Adet");
        s.setFiyat(BigDecimal.valueOf(100));
        s.setMiktar(BigDecimal.valueOf(50));
        s.setMinMiktar(BigDecimal.valueOf(5));
        s.setOlusturmaTarihi(LocalDateTime.now());
        return s;
    }

    @Test
    void tumunuGetir_returnsAll() {
        when(stokRepository.findAllByOrderByAd()).thenReturn(List.of(createStok(1L), createStok(2L)));
        var result = stokService.tumunuGetir(1L, org.springframework.data.domain.Pageable.unpaged());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void ara_returnsFiltered() {
        when(stokRepository.findByAdContainingIgnoreCase("urun")).thenReturn(List.of(createStok(1L)));
        var result = stokService.ara("urun");
        assertEquals(1, result.size());
    }

    @Test
    void getir_returnsStok() {
        when(stokRepository.findById(1L)).thenReturn(Optional.of(createStok(1L)));
        var result = stokService.getir(1L);
        assertEquals("STK001", result.getStokKodu());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(stokRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> stokService.getir(99L));
    }

    @Test
    void olustur_creates() {
        StokDTO dto = StokDTO.builder().stokKodu("STK999").ad("Yeni Urun").birim("KG")
                .fiyat(BigDecimal.valueOf(50)).miktar(BigDecimal.valueOf(100)).minMiktar(BigDecimal.valueOf(10)).build();
        Stok saved = createStok(1L);
        saved.setStokKodu("STK999");
        saved.setAd("Yeni Urun");
        when(stokRepository.save(any(Stok.class))).thenReturn(saved);
        var result = stokService.olustur(dto, 1L);
        assertEquals("STK999", result.getStokKodu());
    }

    @Test
    void guncelle_updates() {
        Stok existing = createStok(1L);
        when(stokRepository.findById(1L)).thenReturn(Optional.of(existing));
        StokDTO dto = StokDTO.builder().stokKodu("STK001").ad("Guncel Urun").birim("Adet")
                .fiyat(BigDecimal.valueOf(150)).miktar(BigDecimal.valueOf(200)).minMiktar(BigDecimal.valueOf(20)).build();
        when(stokRepository.save(any(Stok.class))).thenReturn(existing);
        var result = stokService.guncelle(1L, dto);
        assertEquals("Guncel Urun", result.getAd());
    }

    @Test
    void guncelle_throwsWhenNotFound() {
        when(stokRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> stokService.guncelle(99L, new StokDTO()));
    }

    @Test
    void sil_deletes() {
        when(stokHareketRepository.countByStokId(1L)).thenReturn(0L);
        stokService.sil(1L);
        verify(stokRepository).deleteById(1L);
    }

    @Test
    void sil_throwsWhenHasHareket() {
        when(stokHareketRepository.countByStokId(1L)).thenReturn(2L);
        assertThrows(RuntimeException.class, () -> stokService.sil(1L));
    }

    @Test
    void hareketler_returnsHareketler() {
        Stok stok = createStok(1L);
        StokHareket h = StokHareket.builder().id(1L).stok(stok).tur("GIRIS").miktar(BigDecimal.valueOf(10))
                .hareketTarihi(LocalDate.now()).build();
        when(stokHareketRepository.findByStokIdOrderByHareketTarihiDesc(1L)).thenReturn(List.of(h));
        var result = stokService.hareketler(1L);
        assertEquals(1, result.size());
    }

    @Test
    void tumHareketler_returnsAll() {
        Stok stok = createStok(1L);
        when(stokHareketRepository.findAllByOrderByHareketTarihiDesc())
                .thenReturn(List.of(StokHareket.builder().id(1L).stok(stok).tur("GIRIS").miktar(BigDecimal.valueOf(5)).hareketTarihi(LocalDate.now()).build()));
        var result = stokService.tumHareketler();
        assertEquals(1, result.size());
    }

    @Test
    void hareketEkle_giris_artirirMiktari() {
        Stok stok = createStok(1L);
        when(stokRepository.findById(1L)).thenReturn(Optional.of(stok));
        StokHareketDTO dto = StokHareketDTO.builder().stokId(1L).tur("GIRIS").miktar(BigDecimal.valueOf(10))
                .hareketTarihi(LocalDate.now()).build();
        StokHareket hareket = StokHareket.builder().id(1L).stok(stok).tur("GIRIS").miktar(BigDecimal.valueOf(10))
                .hareketTarihi(LocalDate.now()).build();
        when(stokHareketRepository.save(any(StokHareket.class))).thenReturn(hareket);
        var result = stokService.hareketEkle(dto);
        assertEquals(BigDecimal.valueOf(60), stok.getMiktar());
        assertNotNull(result);
    }

    @Test
    void hareketEkle_cikis_throwsWhenYetersiz() {
        Stok stok = createStok(1L);
        stok.setMiktar(BigDecimal.valueOf(5));
        when(stokRepository.findById(1L)).thenReturn(Optional.of(stok));
        StokHareketDTO dto = StokHareketDTO.builder().stokId(1L).tur("CIKIS").miktar(BigDecimal.valueOf(10))
                .hareketTarihi(LocalDate.now()).build();
        assertThrows(RuntimeException.class, () -> stokService.hareketEkle(dto));
    }

    @Test
    void hareketEkle_cikis_azaltirMiktari() {
        Stok stok = createStok(1L);
        stok.setMiktar(BigDecimal.valueOf(50));
        when(stokRepository.findById(1L)).thenReturn(Optional.of(stok));
        StokHareketDTO dto = StokHareketDTO.builder().stokId(1L).tur("CIKIS").miktar(BigDecimal.valueOf(10))
                .hareketTarihi(LocalDate.now()).build();
        StokHareket hareket = StokHareket.builder().id(1L).stok(stok).tur("CIKIS").miktar(BigDecimal.valueOf(10))
                .hareketTarihi(LocalDate.now()).build();
        when(stokHareketRepository.save(any(StokHareket.class))).thenReturn(hareket);
        stokService.hareketEkle(dto);
        assertEquals(BigDecimal.valueOf(40), stok.getMiktar());
    }

    @Test
    void hareketSil_gerialir() {
        Stok stok = createStok(1L);
        stok.setMiktar(BigDecimal.valueOf(50));
        StokHareket h = StokHareket.builder().id(1L).stok(stok).tur("GIRIS").miktar(BigDecimal.valueOf(10))
                .hareketTarihi(LocalDate.now()).build();
        when(stokHareketRepository.findById(1L)).thenReturn(Optional.of(h));
        stokService.hareketSil(1L);
        assertEquals(BigDecimal.valueOf(40), stok.getMiktar());
        verify(stokHareketRepository).deleteById(1L);
    }

    @Test
    void toplamStokAdet_returnsCount() {
        when(stokRepository.count()).thenReturn(3L);
        assertEquals(3L, stokService.toplamStokAdet());
    }

    @Test
    void toplamStokMiktari_returnsSum() {
        when(stokRepository.toplamMiktar()).thenReturn(BigDecimal.valueOf(100));
        BigDecimal total = stokService.toplamStokMiktari();
        assertEquals(BigDecimal.valueOf(100), total);
    }
}
