package com.raspel.erp.service;

import com.raspel.erp.dto.BankaDTO;
import com.raspel.erp.entity.Banka;
import com.raspel.erp.repository.BankaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankaServiceTest {

    @Mock private BankaRepository bankaRepository;
    @InjectMocks private BankaService bankaService;

    private Banka createBanka(Long id) {
        Banka b = new Banka();
        b.setId(id);
        b.setAd("Test Bankasi");
        b.setHesapNo("12345");
        b.setIban("TR123456789");
        b.setBakiye(BigDecimal.valueOf(10000));
        b.setOlusturmaTarihi(LocalDateTime.now());
        return b;
    }

    @Test
    void tumBankalariGetir_returnsAll() {
        when(bankaRepository.findBySirketId(anyLong())).thenReturn(List.of(createBanka(1L), createBanka(2L)));
        var result = bankaService.tumBankalariGetir(1L);
        assertEquals(2, result.size());
    }

    @Test
    void bankaGetir_returnsBanka() {
        when(bankaRepository.findById(1L)).thenReturn(Optional.of(createBanka(1L)));
        var result = bankaService.bankaGetir(1L);
        assertEquals("Test Bankasi", result.getAd());
    }

    @Test
    void bankaGetir_throwsWhenNotFound() {
        when(bankaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> bankaService.bankaGetir(99L));
    }

    @Test
    void bankaOlustur_creates() {
        BankaDTO dto = BankaDTO.builder().ad("Yeni Banka").hesapNo("54321").iban("TR987654321").build();
        Banka saved = createBanka(1L);
        saved.setAd("Yeni Banka");
        when(bankaRepository.save(any(Banka.class))).thenReturn(saved);
        var result = bankaService.bankaOlustur(dto, 1L);
        assertEquals("Yeni Banka", result.getAd());
    }

    @Test
    void bankaGuncelle_updates() {
        Banka existing = createBanka(1L);
        when(bankaRepository.findById(1L)).thenReturn(Optional.of(existing));
        BankaDTO dto = BankaDTO.builder().ad("Guncel Banka").hesapNo("11111").iban("TR111").build();
        when(bankaRepository.save(any(Banka.class))).thenReturn(existing);
        var result = bankaService.bankaGuncelle(1L, dto);
        assertEquals("Guncel Banka", result.getAd());
    }

    @Test
    void bankaGuncelle_throwsWhenNotFound() {
        when(bankaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> bankaService.bankaGuncelle(99L, new BankaDTO()));
    }

    @Test
    void bankaSil_deletes() {
        when(bankaRepository.existsById(1L)).thenReturn(true);
        bankaService.bankaSil(1L);
        verify(bankaRepository).deleteById(1L);
    }

    @Test
    void bankaSil_throwsWhenNotFound() {
        when(bankaRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> bankaService.bankaSil(99L));
    }
}
