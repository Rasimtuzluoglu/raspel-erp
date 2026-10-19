package com.raspel.erp.service.sistem;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sistem.DonemDTO;
import com.raspel.erp.entity.sistem.Donem;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.DonemKapanisRepository;
import com.raspel.erp.repository.sistem.DonemRepository;
import com.raspel.erp.service.muhasebe.OtomatikMuhasebeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonemServiceTest {

    @Mock private DonemRepository donemRepository;
    @Mock private DonemKapanisRepository donemKapanisRepository;
    @Mock private TenantChecker tenantChecker;
    @Mock private OtomatikMuhasebeService otomatikMuhasebeService;
    @InjectMocks private DonemService donemService;

    private DonemDTO dto(String ad, LocalDate bas, LocalDate bit) {
        return DonemDTO.builder().sirketId(1L).ad(ad).baslangic(bas).bitis(bit).build();
    }

    @Test
    void olustur_cakisanDonemReddedilir() {
        LocalDate bas = LocalDate.of(2026, 1, 1);
        LocalDate bit = LocalDate.of(2026, 12, 31);
        when(donemRepository.cakisanDonemSayisi(1L, null, bas, bit)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> donemService.olustur(dto("2026", bas, bit)));
        verify(donemRepository, never()).save(any());
    }

    @Test
    void olustur_gecersizAralikReddedilir() {
        assertThrows(BusinessException.class, () ->
                donemService.olustur(dto("2026", LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1))));
        verify(donemRepository, never()).save(any());
    }

    @Test
    void olustur_bosAdReddedilir() {
        assertThrows(BusinessException.class, () ->
                donemService.olustur(dto("  ", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))));
    }

    @Test
    void guncelle_cakismadaKendisiniHaricTutar() {
        Donem mevcut = Donem.builder()
                .id(5L).sirketId(1L).ad("2026")
                .baslangic(LocalDate.of(2026, 1, 1)).bitis(LocalDate.of(2026, 12, 31))
                .aktif(true).kilitli(false)
                .build();
        when(donemRepository.findById(5L)).thenReturn(Optional.of(mevcut));
        when(donemRepository.cakisanDonemSayisi(eq(1L), eq(5L), any(), any())).thenReturn(0L);
        when(donemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DonemDTO sonuc = donemService.guncelle(5L, DonemDTO.builder().ad("2026 Güncel").build());

        assertEquals("2026 Güncel", sonuc.getAd());
        verify(donemRepository).cakisanDonemSayisi(
                1L, 5L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
    }

    @Test
    void kapanislariGetir_kilitlenenDonemSayisiniHesaplar() {
        Donem d = Donem.builder()
                .id(1L).sirketId(1L).ad("2025")
                .baslangic(LocalDate.of(2025, 1, 1)).bitis(LocalDate.of(2025, 12, 31))
                .build();
        when(donemRepository.findBySirketIdOrderByBaslangicDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(d)));
        when(donemKapanisRepository.findBySirketIdOrderByYilDesc(1L))
                .thenReturn(List.of(com.raspel.erp.entity.sistem.DonemKapanis.builder()
                        .id(9L).sirketId(1L).yil(2025).build()));

        var sonuc = donemService.kapanislariGetir(1L);

        assertEquals(1, sonuc.size());
        assertEquals(1, sonuc.get(0).getKilitlenenDonemSayisi());
    }
}
