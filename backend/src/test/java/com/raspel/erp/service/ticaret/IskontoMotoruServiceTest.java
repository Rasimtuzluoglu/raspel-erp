package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.IskontoKuraliDTO;
import com.raspel.erp.entity.ticaret.IskontoKurali;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ticaret.IskontoKuraliRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IskontoMotoruServiceTest {

    @Mock private IskontoKuraliRepository iskontoKuraliRepository;
    @Mock private TenantChecker tenantChecker;
    @InjectMocks private IskontoMotoruService service;

    private IskontoKurali kural(Long id, Long stokId, BigDecimal minAdet, BigDecimal maxAdet,
                                BigDecimal oran, Integer oncelik) {
        return IskontoKurali.builder()
                .id(id).sirketId(1L).ad("Kural " + id)
                .stokId(stokId).minAdet(minAdet).maxAdet(maxAdet)
                .iskontoOrani(oran).oncelik(oncelik).aktif(true).build();
    }

    @Test
    void tumunuGetir_returnsPage() {
        when(iskontoKuraliRepository.findBySirketIdOrderByOncelikAscIdDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(kural(1L, null, null, null, BigDecimal.TEN, 100))));
        var result = service.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(iskontoKuraliRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getir(99L));
    }

    @Test
    void olustur_creates() {
        IskontoKuraliDTO dto = IskontoKuraliDTO.builder()
                .sirketId(1L).ad("Kademeli").iskontoOrani(new BigDecimal("15")).build();
        when(iskontoKuraliRepository.save(any(IskontoKurali.class))).thenAnswer(i -> i.getArgument(0));
        var result = service.olustur(dto);
        assertEquals("Kademeli", result.getAd());
        assertEquals(100, result.getOncelik());
    }

    @Test
    void olustur_gecersizOranReddedilir() {
        IskontoKuraliDTO dto = IskontoKuraliDTO.builder()
                .sirketId(1L).ad("X").iskontoOrani(new BigDecimal("150")).build();
        assertThrows(BusinessException.class, () -> service.olustur(dto));
    }

    @Test
    void iskontoHesapla_enYuksekOncelikliKuralSecilir() {
        when(iskontoKuraliRepository.findBySirketIdAndAktifTrue(1L)).thenReturn(List.of(
                kural(1L, 10L, new BigDecimal("1"), new BigDecimal("10"), new BigDecimal("5"), 100),
                kural(2L, 10L, new BigDecimal("11"), new BigDecimal("100"), new BigDecimal("12"), 50)));

        BigDecimal oran = service.iskontoHesapla(1L, 10L, null, null, new BigDecimal("20"), LocalDate.now());

        assertEquals(0, new BigDecimal("12").compareTo(oran));
    }

    @Test
    void iskontoHesapla_ayniOncelikteYuksekOranKazanir() {
        when(iskontoKuraliRepository.findBySirketIdAndAktifTrue(1L)).thenReturn(List.of(
                kural(1L, 10L, null, null, new BigDecimal("5"), 100),
                kural(2L, 10L, null, null, new BigDecimal("8"), 100)));

        BigDecimal oran = service.iskontoHesapla(1L, 10L, null, null, BigDecimal.ONE, LocalDate.now());

        assertEquals(0, new BigDecimal("8").compareTo(oran));
    }

    @Test
    void iskontoHesapla_kapsamDisiKuralUygulanmaz() {
        when(iskontoKuraliRepository.findBySirketIdAndAktifTrue(1L)).thenReturn(List.of(
                kural(1L, 99L, null, null, new BigDecimal("20"), 100)));

        BigDecimal oran = service.iskontoHesapla(1L, 10L, null, null, BigDecimal.ONE, LocalDate.now());

        assertEquals(0, BigDecimal.ZERO.compareTo(oran));
    }

    @Test
    void iskontoHesapla_tarihDisiKuralUygulanmaz() {
        IskontoKurali gecmis = kural(1L, 10L, null, null, new BigDecimal("20"), 100);
        gecmis.setGecerliBitis(LocalDate.now().minusDays(1));
        when(iskontoKuraliRepository.findBySirketIdAndAktifTrue(1L)).thenReturn(List.of(gecmis));

        BigDecimal oran = service.iskontoHesapla(1L, 10L, null, null, BigDecimal.ONE, LocalDate.now());

        assertEquals(0, BigDecimal.ZERO.compareTo(oran));
    }

    @Test
    void iskontoHesapla_cariVeKategoriKapsami() {
        IskontoKurali k = kural(1L, null, null, null, new BigDecimal("10"), 100);
        k.setCariHesapId(7L);
        k.setKategori("Elektronik");
        when(iskontoKuraliRepository.findBySirketIdAndAktifTrue(1L)).thenReturn(List.of(k));

        assertEquals(0, new BigDecimal("10").compareTo(
                service.iskontoHesapla(1L, null, 7L, "Elektronik", BigDecimal.ONE, LocalDate.now())));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                service.iskontoHesapla(1L, null, 8L, "Elektronik", BigDecimal.ONE, LocalDate.now())));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                service.iskontoHesapla(1L, null, 7L, "Gida", BigDecimal.ONE, LocalDate.now())));
    }

    @Test
    void sil_deletes() {
        when(iskontoKuraliRepository.findById(1L)).thenReturn(Optional.of(kural(1L, null, null, null, BigDecimal.ONE, 100)));
        service.sil(1L);
        verify(iskontoKuraliRepository).delete(any(IskontoKurali.class));
    }
}
