package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.MasrafDTO;
import com.raspel.erp.entity.finans.Masraf;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.MasrafRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasrafServiceTest {

    @Mock private MasrafRepository masrafRepository;
    @Mock private com.raspel.erp.service.sistem.AuditLogService auditLogService;
    @Mock private TenantChecker tenantChecker;
    @Mock private com.raspel.erp.config.CacheYardimci cacheYardimci;
    @Mock private com.raspel.erp.repository.finans.KasaRepository kasaRepository;
    @Mock private com.raspel.erp.repository.finans.KasaHareketRepository kasaHareketRepository;
    @Mock private com.raspel.erp.repository.finans.BankaRepository bankaRepository;
    @Mock private com.raspel.erp.repository.finans.BankaHareketiRepository bankaHareketiRepository;
    @Mock private com.raspel.erp.service.sistem.DonemService donemService;
    @Mock private com.raspel.erp.service.muhasebe.OtomatikMuhasebeService otomatikMuhasebeService;
    @Mock private com.raspel.erp.repository.finans.CariHesapRepository cariHesapRepository;
    @InjectMocks private MasrafService masrafService;

    private Masraf ornekMasraf(Long id) {
        return Masraf.builder()
                .id(id).tarih(LocalDate.now()).tutar(new BigDecimal("750"))
                .aciklama("Kırtasiye").kategori("Ofis").sirketId(1L)
                .olusturmaTarihi(LocalDateTime.now()).build();
    }

    @Test
    void tumunuGetir_returnsPage() {
        when(masrafRepository.findBySirketIdOrderByTarihDesc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornekMasraf(1L))));
        var sonuc = masrafService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, sonuc.getContent().size());
    }

    @Test
    void getir_returnsById() {
        when(masrafRepository.findById(1L)).thenReturn(Optional.of(ornekMasraf(1L)));
        var sonuc = masrafService.getir(1L);
        assertEquals("Kırtasiye", sonuc.getAciklama());
    }

    @Test
    void getir_notFound_throws() {
        when(masrafRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> masrafService.getir(99L));
    }

    @Test
    void olustur_creates() {
        MasrafDTO dto = MasrafDTO.builder().tarih(LocalDate.now())
                .tutar(new BigDecimal("300")).aciklama("Yakıt").kategori("Araç").build();
        when(masrafRepository.save(any(Masraf.class))).thenAnswer(inv -> {
            Masraf m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });
        var sonuc = masrafService.olustur(dto, 1L);
        assertEquals("Yakıt", sonuc.getAciklama());
        assertEquals(1L, sonuc.getSirketId());
    }

    @Test
    void olustur_kdvAyristirilirVeKasadanOdemeIslenir() {
        MasrafDTO dto = MasrafDTO.builder().tarih(LocalDate.now())
                .tutar(new BigDecimal("120")).kdvOrani(new BigDecimal("20"))
                .kasaId(5L).aciklama("Yakıt").kategori("YAKIT").build();
        when(masrafRepository.save(any(Masraf.class))).thenAnswer(inv -> {
            Masraf m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });
        com.raspel.erp.entity.finans.Kasa kasa = com.raspel.erp.entity.finans.Kasa.builder()
                .id(5L).ad("Merkez").bakiye(new BigDecimal("1000")).sirketId(1L).build();
        when(kasaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(kasa));

        var sonuc = masrafService.olustur(dto, 1L);

        // 120 TL KDV dahil: matrah 100, KDV 20; kasa 880'e düşer.
        assertEquals(0, sonuc.getMatrah().compareTo(new BigDecimal("100")));
        assertEquals(0, sonuc.getKdvTutar().compareTo(new BigDecimal("20")));
        assertEquals(0, kasa.getBakiye().compareTo(new BigDecimal("880")));
        verify(kasaHareketRepository).save(any(com.raspel.erp.entity.finans.KasaHareket.class));
        verify(otomatikMuhasebeService).masrafIsle(any(Masraf.class));
    }

    @Test
    void olustur_hemKasaHemBankaReddedilir() {
        MasrafDTO dto = MasrafDTO.builder().tarih(LocalDate.now())
                .tutar(new BigDecimal("100")).kasaId(5L).bankaId(6L).build();
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> masrafService.olustur(dto, 1L));
    }

    @Test
    void guncelle_updates() {
        Masraf masraf = ornekMasraf(1L);
        when(masrafRepository.findById(1L)).thenReturn(Optional.of(masraf));
        when(masrafRepository.save(any(Masraf.class))).thenAnswer(inv -> inv.getArgument(0));

        MasrafDTO dto = MasrafDTO.builder().tutar(new BigDecimal("999")).build();
        var sonuc = masrafService.guncelle(1L, dto);
        assertEquals(0, sonuc.getTutar().compareTo(new BigDecimal("999")));
    }

    @Test
    void sil_deletes() {
        when(masrafRepository.findById(1L)).thenReturn(Optional.of(ornekMasraf(1L)));
        masrafService.sil(1L);
        verify(masrafRepository).deleteById(1L);
    }

    @Test
    void sil_notFound_throws() {
        when(masrafRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> masrafService.sil(99L));
    }

    @Test
    void ozet_toplamlariDoner() {
        when(masrafRepository.sumTutarBySirketIdAndTarihBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("1000"), new BigDecimal("5000"));
        when(masrafRepository.sumKdvBySirketIdAndTarihBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("900"));
        when(masrafRepository.countBySirketId(1L)).thenReturn(12L);

        var ozet = masrafService.ozet(1L);

        assertEquals(new BigDecimal("1000"), ozet.getBuAyToplam());
        assertEquals(new BigDecimal("5000"), ozet.getBuYilToplam());
        assertEquals(new BigDecimal("900"), ozet.getBuYilKdv());
        assertEquals(12L, ozet.getToplamKayit());
    }

    @Test
    void ozet_sirketYoksaSifir() {
        var ozet = masrafService.ozet(null);
        assertEquals(BigDecimal.ZERO, ozet.getBuAyToplam());
        assertEquals(BigDecimal.ZERO, ozet.getBuYilToplam());
        assertEquals(BigDecimal.ZERO, ozet.getBuYilKdv());
        assertEquals(0L, ozet.getToplamKayit());
        verifyNoInteractions(masrafRepository);
    }

    @Test
    void filtreli_sirketYoksaBos() {
        var sonuc = masrafService.filtreli(null, "OFIS", null, null, "abc", Pageable.unpaged());
        assertTrue(sonuc.isEmpty());
        verifyNoInteractions(masrafRepository);
    }

    @Test
    void filtreli_sayfalar() {
        when(masrafRepository.filtrele(eq(1L), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornekMasraf(1L))));

        var sonuc = masrafService.filtreli(1L, "TUMU", null, null, "   ", Pageable.unpaged());

        assertEquals(1, sonuc.getContent().size());
    }

    @Test
    void filtreli_kategoriVeAramaNormallestirilir() {
        when(masrafRepository.filtrele(eq(1L), eq("OFIS"), isNull(), isNull(), eq("kirtasiye"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        masrafService.filtreli(1L, "ofis", null, null, "  KIRTASIYE  ", Pageable.unpaged());

        verify(masrafRepository).filtrele(eq(1L), eq("OFIS"), isNull(), isNull(), eq("kirtasiye"), any(Pageable.class));
    }

    @Test
    void olustur_kategoriZorunlu() {
        MasrafDTO dto = MasrafDTO.builder().tarih(LocalDate.now()).tutar(new BigDecimal("100")).build();
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> masrafService.olustur(dto, 1L));
        verify(masrafRepository, never()).save(any(Masraf.class));
    }

    @Test
    void olustur_gecersizOdemeYontemiReddedilir() {
        MasrafDTO dto = MasrafDTO.builder().tarih(LocalDate.now()).tutar(new BigDecimal("100"))
                .kategori("OFIS").odemeYontemi("CEK").build();
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> masrafService.olustur(dto, 1L));
        verify(masrafRepository, never()).save(any(Masraf.class));
    }

    @Test
    void olustur_cariHesapBaskaSirketseReddedilir() {
        com.raspel.erp.entity.finans.CariHesap cari = com.raspel.erp.entity.finans.CariHesap.builder()
                .id(9L).sirketId(2L).build();
        when(cariHesapRepository.findById(9L)).thenReturn(Optional.of(cari));
        doThrow(new ResourceNotFoundException("Cari hesap", 9L))
                .when(tenantChecker).check(2L, "Cari hesap");

        MasrafDTO dto = MasrafDTO.builder().tarih(LocalDate.now()).tutar(new BigDecimal("100"))
                .kategori("OFIS").cariHesapId(9L).build();
        assertThrows(ResourceNotFoundException.class, () -> masrafService.olustur(dto, 1L));
        verify(masrafRepository, never()).save(any(Masraf.class));
    }
}
