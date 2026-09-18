package com.raspel.erp.service;

import com.raspel.erp.dto.sistem.DonemDTO;
import com.raspel.erp.entity.sistem.Donem;
import com.raspel.erp.entity.sistem.DonemKapanis;
import com.raspel.erp.repository.sistem.DonemRepository;
import com.raspel.erp.repository.sistem.DonemKapanisRepository;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.sistem.DonemService;

@ExtendWith(MockitoExtension.class)
class DonemServiceTest {

    @Mock private DonemRepository donemRepository;
    @Mock private DonemKapanisRepository donemKapanisRepository;
    @Mock private TenantChecker tenantChecker;
    @InjectMocks private DonemService donemService;

    private Donem createDonem(Long id) {
        Donem d = new Donem();
        d.setId(id);
        d.setSirketId(1L);
        d.setAd("2026-1");
        d.setBaslangic(LocalDate.of(2026, 1, 1));
        d.setBitis(LocalDate.of(2026, 6, 30));
        d.setAktif(true);
        d.setOlusturmaTarihi(LocalDateTime.now());
        return d;
    }

    @Test
    void tumunuGetir_returnsAll() {
        when(donemRepository.findBySirketIdOrderByBaslangicDesc(1L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(createDonem(1L), createDonem(2L))));
        var result = donemService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void sirketeGoreGetir_returnsForSirket() {
        when(donemRepository.findBySirketIdOrderByBaslangicDesc(1L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(createDonem(1L))));
        var result = donemService.sirketeGoreGetir(1L);
        assertEquals(1, result.size());
    }

    @Test
    void aktifDonemler_returnsActive() {
        when(donemRepository.findBySirketIdAndAktifTrue(1L)).thenReturn(List.of(createDonem(1L)));
        var result = donemService.aktifDonemler(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getir_returnsDonem() {
        when(donemRepository.findById(1L)).thenReturn(Optional.of(createDonem(1L)));
        var result = donemService.getir(1L);
        assertEquals("2026-1", result.getAd());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(donemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> donemService.getir(99L));
    }

    @Test
    void olustur_creates() {
        DonemDTO dto = DonemDTO.builder().sirketId(1L).ad("2026-2")
                .baslangic(LocalDate.of(2026, 7, 1)).bitis(LocalDate.of(2026, 12, 31)).build();
        Donem saved = createDonem(1L);
        saved.setAd("2026-2");
        when(donemRepository.save(any(Donem.class))).thenReturn(saved);
        var result = donemService.olustur(dto);
        assertEquals("2026-2", result.getAd());
    }

    @Test
    void guncelle_updates() {
        Donem existing = createDonem(1L);
        when(donemRepository.findById(1L)).thenReturn(Optional.of(existing));
        DonemDTO dto = DonemDTO.builder().ad("Guncel").aktif(false).build();
        when(donemRepository.save(any(Donem.class))).thenReturn(existing);
        var result = donemService.guncelle(1L, dto);
        assertEquals("Guncel", result.getAd());
    }

    @Test
    void guncelle_throwsWhenNotFound() {
        when(donemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> donemService.guncelle(99L, new DonemDTO()));
    }

    @Test
    void sil_deletes() {
        when(donemRepository.findById(1L)).thenReturn(Optional.of(createDonem(1L)));
        donemService.sil(1L);
        verify(donemRepository).deleteById(1L);
    }

    @Test
    void sil_throwsWhenNotFound() {
        when(donemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> donemService.sil(99L));
    }

    @Test
    void sil_kilitliDonemReddedilir() {
        Donem d = createDonem(1L);
        d.setKilitli(true);
        when(donemRepository.findById(1L)).thenReturn(Optional.of(d));

        assertThrows(BusinessException.class, () -> donemService.sil(1L));
        verify(donemRepository, never()).deleteById(any());
    }

    @Test
    void tarihKilitliMi_kilitliAraliktaTrueDoner() {
        Donem d = createDonem(1L);
        d.setKilitli(true);
        when(donemRepository.findBySirketIdAndKilitliTrue(1L)).thenReturn(List.of(d));

        assertTrue(donemService.tarihKilitliMi(1L, LocalDate.of(2026, 3, 15)));
        assertFalse(donemService.tarihKilitliMi(1L, LocalDate.of(2026, 8, 15)));
    }

    @Test
    void kilitle_veKilidiAc() {
        Donem d = createDonem(1L);
        when(donemRepository.findById(1L)).thenReturn(Optional.of(d));
        when(donemRepository.save(any(Donem.class))).thenAnswer(i -> i.getArgument(0));

        var kilitli = donemService.kilitle(1L);
        assertTrue(kilitli.getKilitli());
        assertNotNull(kilitli.getKilitTarihi());

        var acik = donemService.kilidiAc(1L);
        assertFalse(acik.getKilitli());
        assertNull(acik.getKilitTarihi());
    }

    @Test
    void yilSonuKapat_donemleriKilitlerVeKaydeder() {
        when(donemRepository.findBySirketIdOrderByBaslangicDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createDonem(1L))));
        when(donemKapanisRepository.findBySirketIdAndYil(1L, 2026)).thenReturn(Optional.empty());
        when(donemKapanisRepository.save(any(DonemKapanis.class))).thenAnswer(i -> {
            DonemKapanis k = i.getArgument(0);
            k.setId(5L);
            return k;
        });
        when(donemRepository.save(any(Donem.class))).thenAnswer(i -> i.getArgument(0));

        var sonuc = donemService.yilSonuKapat(1L, 2026, "yil sonu", 9L);

        assertEquals(2026, sonuc.getYil());
        assertEquals(1, sonuc.getKilitlenenDonemSayisi());
        verify(donemRepository).save(any(Donem.class));
    }

    @Test
    void yilSonuKapat_ikinciKezReddedilir() {
        when(donemKapanisRepository.findBySirketIdAndYil(1L, 2026))
                .thenReturn(Optional.of(DonemKapanis.builder().id(1L).sirketId(1L).yil(2026).build()));

        assertThrows(BusinessException.class, () -> donemService.yilSonuKapat(1L, 2026, null, null));
    }

    @Test
    void yilSonuKapat_donemYoksaHataVerir() {
        when(donemKapanisRepository.findBySirketIdAndYil(1L, 2030)).thenReturn(Optional.empty());
        when(donemRepository.findBySirketIdOrderByBaslangicDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThrows(BusinessException.class, () -> donemService.yilSonuKapat(1L, 2030, null, null));
    }
}