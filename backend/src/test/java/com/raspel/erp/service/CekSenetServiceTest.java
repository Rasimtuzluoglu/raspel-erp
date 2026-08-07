package com.raspel.erp.service;

import com.raspel.erp.dto.finans.CekSenetDTO;
import com.raspel.erp.entity.finans.CekSenet;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.CekSenetRepository;
import com.raspel.erp.config.TenantChecker;
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
import com.raspel.erp.service.finans.CekSenetService;

@ExtendWith(MockitoExtension.class)
class CekSenetServiceTest {

    @Mock private CekSenetRepository cekSenetRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private TenantChecker tenantChecker;
    @InjectMocks private CekSenetService cekSenetService;

    private CekSenet createCekSenet(Long id) {
        CekSenet cs = new CekSenet();
        cs.setId(id);
        cs.setTur("CEK");
        cs.setCariHesapId(1L);
        cs.setBankaAdi("Is Bankasi");
        cs.setCekNo("12345");
        cs.setVadeTarihi(LocalDate.now().plusMonths(3));
        cs.setTutar(BigDecimal.valueOf(10000));
        cs.setDurum("PORTFOY");
        cs.setSirketId(1L);
        cs.setOlusturmaTarihi(LocalDateTime.now());
        return cs;
    }

    @Test
    void tumunuGetir_returnsAll() {
        when(cekSenetRepository.findBySirketIdOrderByVadeTarihiAsc(1L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(createCekSenet(1L))));
        var result = cekSenetService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getir_returnsCekSenet() {
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(createCekSenet(1L)));
        var result = cekSenetService.getir(1L);
        assertEquals("CEK", result.getTur());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(cekSenetRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> cekSenetService.getir(99L));
    }

    @Test
    void olustur_creates() {
        CekSenetDTO dto = CekSenetDTO.builder().tur("SENET").cariHesapId(1L)
                .vadeTarihi(LocalDate.now().plusMonths(2)).tutar(BigDecimal.valueOf(5000)).sirketId(1L).build();
        CekSenet saved = createCekSenet(1L);
        saved.setTur("SENET");
        when(cekSenetRepository.save(any(CekSenet.class))).thenReturn(saved);
        var result = cekSenetService.olustur(dto);
        assertEquals("SENET", result.getTur());
        assertEquals("PORTFOY", result.getDurum());
    }

    @Test
    void durumGuncelle_updates() {
        CekSenet cs = createCekSenet(1L);
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(cs));
        when(cekSenetRepository.save(any(CekSenet.class))).thenReturn(cs);
        var result = cekSenetService.durumGuncelle(1L, "TAHSIL_EDILDI");
        assertEquals("TAHSIL_EDILDI", result.getDurum());
    }

    @Test
    void durumGuncelle_throwsWhenNotFound() {
        when(cekSenetRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> cekSenetService.durumGuncelle(99L, "TAHSIL_EDILDI"));
    }

    @Test
    void sil_deletes() {
        when(cekSenetRepository.findById(1L)).thenReturn(Optional.of(createCekSenet(1L)));
        cekSenetService.sil(1L);
        verify(cekSenetRepository).deleteById(1L);
    }
}