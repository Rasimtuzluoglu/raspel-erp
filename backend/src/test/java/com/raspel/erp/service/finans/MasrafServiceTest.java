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
                .kasaId(5L).aciklama("Yakıt").build();
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
}
