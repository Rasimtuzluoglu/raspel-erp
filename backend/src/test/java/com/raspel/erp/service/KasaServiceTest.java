package com.raspel.erp.service;

import com.raspel.erp.dto.KasaDTO;
import com.raspel.erp.dto.KasaHareketDTO;
import com.raspel.erp.entity.*;
import com.raspel.erp.repository.*;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KasaServiceTest {

    @Mock private KasaRepository kasaRepository;
    @Mock private KasaHareketRepository kasaHareketRepository;
    @Mock private KategoriRepository kategoriRepository;
    @InjectMocks private KasaService kasaService;

    private Kasa createKasa(Long id) {
        Kasa k = new Kasa();
        k.setId(id);
        k.setAd("Kasa " + id);
        k.setBakiye(BigDecimal.valueOf(5000));
        k.setOlusturmaTarihi(LocalDateTime.now());
        return k;
    }

    @Test
    void tumKasalarGetir_returnsAll() {
        when(kasaRepository.findBySirketId(anyLong())).thenReturn(List.of(createKasa(1L), createKasa(2L)));
        var result = kasaService.tumKasalarGetir(1L);
        assertEquals(2, result.size());
    }

    @Test
    void kasaGetir_returnsKasa() {
        when(kasaRepository.findById(1L)).thenReturn(Optional.of(createKasa(1L)));
        var result = kasaService.kasaGetir(1L);
        assertEquals("Kasa 1", result.getAd());
    }

    @Test
    void kasaGetir_throwsWhenNotFound() {
        when(kasaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> kasaService.kasaGetir(99L));
    }

    @Test
    void kasaOlustur_creates() {
        KasaDTO dto = KasaDTO.builder().ad("Yeni Kasa").build();
        Kasa saved = createKasa(1L);
        saved.setAd("Yeni Kasa");
        when(kasaRepository.save(any(Kasa.class))).thenReturn(saved);
        var result = kasaService.kasaOlustur(dto, 1L);
        assertEquals("Yeni Kasa", result.getAd());
    }

    @Test
    void kasaGuncelle_updates() {
        Kasa existing = createKasa(1L);
        when(kasaRepository.findById(1L)).thenReturn(Optional.of(existing));
        KasaDTO dto = KasaDTO.builder().ad("Guncel Kasa").build();
        when(kasaRepository.save(any(Kasa.class))).thenReturn(existing);
        var result = kasaService.kasaGuncelle(1L, dto);
        assertEquals("Guncel Kasa", result.getAd());
    }

    @Test
    void kasaGuncelle_throwsWhenNotFound() {
        when(kasaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> kasaService.kasaGuncelle(99L, new KasaDTO()));
    }

    @Test
    void kasaSil_deletes() {
        when(kasaHareketRepository.countByKasaId(1L)).thenReturn(0L);
        kasaService.kasaSil(1L);
        verify(kasaRepository).deleteById(1L);
    }

    @Test
    void kasaSil_throwsWhenHasHareket() {
        when(kasaHareketRepository.countByKasaId(1L)).thenReturn(2L);
        assertThrows(RuntimeException.class, () -> kasaService.kasaSil(1L));
    }

    @Test
    void kasaHareketleriGetir_returnsHareketler() {
        Kasa kasa = createKasa(1L);
        KasaHareket h = KasaHareket.builder().id(1L).kasa(kasa).tur("GELIR")
                .tutar(BigDecimal.valueOf(1000)).hareketTarihi(LocalDate.now()).build();
        when(kasaHareketRepository.findByKasaIdOrderByHareketTarihiDesc(1L)).thenReturn(List.of(h));
        var result = kasaService.kasaHareketleriGetir(1L);
        assertEquals(1, result.size());
    }

    @Test
    void hareketEkle_gelir_artirirBakiye() {
        Kasa kasa = createKasa(1L);
        kasa.setBakiye(BigDecimal.valueOf(5000));
        when(kasaRepository.findById(1L)).thenReturn(Optional.of(kasa));
        KasaHareketDTO dto = KasaHareketDTO.builder().kasaId(1L).tur("GELIR")
                .tutar(BigDecimal.valueOf(1000)).hareketTarihi(LocalDate.now()).build();
        KasaHareket h = KasaHareket.builder().id(1L).kasa(kasa).tur("GELIR")
                .tutar(BigDecimal.valueOf(1000)).hareketTarihi(LocalDate.now()).build();
        when(kasaHareketRepository.save(any(KasaHareket.class))).thenReturn(h);
        var result = kasaService.hareketEkle(dto);
        assertEquals(BigDecimal.valueOf(6000), kasa.getBakiye());
        assertNotNull(result);
    }

    @Test
    void hareketEkle_gider_azaltirBakiye() {
        Kasa kasa = createKasa(1L);
        kasa.setBakiye(BigDecimal.valueOf(5000));
        when(kasaRepository.findById(1L)).thenReturn(Optional.of(kasa));
        KasaHareketDTO dto = KasaHareketDTO.builder().kasaId(1L).tur("GIDER")
                .tutar(BigDecimal.valueOf(2000)).hareketTarihi(LocalDate.now()).build();
        KasaHareket h = KasaHareket.builder().id(1L).kasa(kasa).tur("GIDER")
                .tutar(BigDecimal.valueOf(2000)).hareketTarihi(LocalDate.now()).build();
        when(kasaHareketRepository.save(any(KasaHareket.class))).thenReturn(h);
        var result = kasaService.hareketEkle(dto);
        assertEquals(BigDecimal.valueOf(3000), kasa.getBakiye());
    }

    @Test
    void hareketSil_gerialir() {
        Kasa kasa = createKasa(1L);
        kasa.setBakiye(BigDecimal.valueOf(5000));
        KasaHareket h = KasaHareket.builder().id(1L).kasa(kasa).tur("GELIR")
                .tutar(BigDecimal.valueOf(1000)).hareketTarihi(LocalDate.now()).build();
        when(kasaHareketRepository.findById(1L)).thenReturn(Optional.of(h));
        kasaService.hareketSil(1L);
        assertEquals(BigDecimal.valueOf(4000), kasa.getBakiye());
        verify(kasaHareketRepository).deleteById(1L);
    }
}
