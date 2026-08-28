package com.raspel.erp.service;

import com.raspel.erp.dto.finans.BankaDTO;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.config.TenantChecker;
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
import com.raspel.erp.service.finans.BankaService;

@ExtendWith(MockitoExtension.class)
class BankaServiceTest {

    @Mock private BankaRepository bankaRepository;
    @Mock private com.raspel.erp.repository.finans.BankaHareketiRepository bankaHareketiRepository;
    @Mock private TenantChecker tenantChecker;
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
        when(bankaRepository.findBySirketId(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(createBanka(1L), createBanka(2L))));
        var result = bankaService.tumBankalariGetir(1L, Pageable.unpaged());
        assertEquals(2, result.getContent().size());
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
        Banka b = createBanka(1L);
        b.setBakiye(BigDecimal.ZERO);
        when(bankaRepository.findById(1L)).thenReturn(Optional.of(b));
        when(bankaHareketiRepository.countByBankaId(1L)).thenReturn(0L);
        bankaService.bankaSil(1L);
        verify(bankaRepository).deleteById(1L);
    }

    @Test
    void bankaSil_throwsWhenHasHareket() {
        Banka b = createBanka(1L);
        b.setBakiye(BigDecimal.ZERO);
        when(bankaRepository.findById(1L)).thenReturn(Optional.of(b));
        when(bankaHareketiRepository.countByBankaId(1L)).thenReturn(4L);
        assertThrows(RuntimeException.class, () -> bankaService.bankaSil(1L));
        verify(bankaRepository, never()).deleteById(anyLong());
    }

    @Test
    void bankaSil_throwsWhenHasBakiye() {
        Banka b = createBanka(1L);
        b.setBakiye(BigDecimal.valueOf(10000));
        when(bankaRepository.findById(1L)).thenReturn(Optional.of(b));
        assertThrows(RuntimeException.class, () -> bankaService.bankaSil(1L));
    }

    @Test
    void bankaSil_throwsWhenNotFound() {
        when(bankaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> bankaService.bankaSil(99L));
    }
}