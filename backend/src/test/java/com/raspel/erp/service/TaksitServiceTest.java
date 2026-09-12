package com.raspel.erp.service;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.TaksitDTO;
import com.raspel.erp.dto.finans.TaksitOdeDTO;
import com.raspel.erp.dto.finans.TaksitPlanDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.Taksit;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.TaksitRepository;
import com.raspel.erp.service.finans.TaksitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaksitServiceTest {

    @Mock private TaksitRepository taksitRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private TenantChecker tenantChecker;
    @InjectMocks private TaksitService taksitService;

    private CariHesap cari(Long id, Long sirketId) {
        return CariHesap.builder().id(id).ad("Test Cari").sirketId(sirketId)
                .bakiye(BigDecimal.ZERO).aktif(true).build();
    }

    private Taksit kalem(Long id, CariHesap cari, LocalDate vade, String tutar, String durum) {
        return Taksit.builder()
                .id(id).sirketId(1L).cariHesap(cari)
                .planNo("TKS-1").taksitNo(1).taksitSayisi(3)
                .vadeTarihi(vade).tutar(new BigDecimal(tutar))
                .odemeDurumu(durum)
                .build();
    }

    @Test
    void planOlustur_tutariVadeBazliBoler() {
        CariHesap c = cari(1L, 1L);
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(c));
        when(taksitRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        TaksitPlanDTO dto = TaksitPlanDTO.builder()
                .cariId(1L).toplamTutar(new BigDecimal("100.00")).taksitSayisi(3)
                .baslangicTarihi(LocalDate.of(2026, 1, 15)).periyotAy(1)
                .kurum("Test Bank").build();

        List<TaksitDTO> sonuc = taksitService.planOlustur(dto, 1L);

        assertEquals(3, sonuc.size());
        assertEquals(new BigDecimal("33.33"), sonuc.get(0).getTutar());
        assertEquals(new BigDecimal("33.33"), sonuc.get(1).getTutar());
        assertEquals(new BigDecimal("33.34"), sonuc.get(2).getTutar());
        assertEquals(LocalDate.of(2026, 1, 15), sonuc.get(0).getVadeTarihi());
        assertEquals(LocalDate.of(2026, 2, 15), sonuc.get(1).getVadeTarihi());
        assertEquals(LocalDate.of(2026, 3, 15), sonuc.get(2).getVadeTarihi());
        assertEquals(1, sonuc.get(0).getTaksitNo());
        assertEquals(sonuc.get(0).getPlanNo(), sonuc.get(1).getPlanNo());
        assertNotNull(sonuc.get(0).getPlanNo());
        verify(tenantChecker).check(1L, "Cari Hesap");
    }

    @Test
    void planOlustur_gecersizTutarHataFirlatir() {
        TaksitPlanDTO dto = TaksitPlanDTO.builder()
                .cariId(1L).toplamTutar(BigDecimal.ZERO).taksitSayisi(3).build();
        assertThrows(BusinessException.class, () -> taksitService.planOlustur(dto, 1L));
    }

    @Test
    void planOlustur_cariBulunamazsaHataFirlatir() {
        when(cariHesapRepository.findById(9L)).thenReturn(Optional.empty());
        TaksitPlanDTO dto = TaksitPlanDTO.builder()
                .cariId(9L).toplamTutar(new BigDecimal("100")).taksitSayisi(2).build();
        assertThrows(ResourceNotFoundException.class, () -> taksitService.planOlustur(dto, 1L));
    }

    @Test
    void yaklasan_gecikmeGunuHesaplar() {
        CariHesap c = cari(1L, 1L);
        LocalDate bugun = LocalDate.now();
        when(taksitRepository.findBySirketIdAndOdemeDurumuNotAndVadeTarihiBetweenOrderByVadeTarihiAsc(
                eq(1L), eq("ODENDI"), any(), any()))
                .thenReturn(List.of(
                        kalem(1L, c, bugun.minusDays(10), "100", "BEKLEMEDE"),
                        kalem(2L, c, bugun.plusDays(5), "200", "BEKLEMEDE")));

        List<TaksitDTO> sonuc = taksitService.yaklasan(1L, 30);

        assertEquals(2, sonuc.size());
        assertEquals(10, sonuc.get(0).getGecikmeGunu());
        assertEquals(0, sonuc.get(1).getGecikmeGunu());
    }

    @Test
    void takvim_ayAraliginiGetirir() {
        CariHesap c = cari(1L, 1L);
        when(taksitRepository.findBySirketIdAndVadeTarihiBetweenOrderByVadeTarihiAsc(eq(1L), any(), any()))
                .thenReturn(List.of(kalem(1L, c, LocalDate.of(2026, 5, 10), "100", "BEKLEMEDE")));

        List<TaksitDTO> sonuc = taksitService.takvim(1L, 2026, 5);

        assertEquals(1, sonuc.size());
        assertEquals(LocalDate.of(2026, 5, 10), sonuc.get(0).getVadeTarihi());
    }

    @Test
    void ozet_bekleyenGecikmisVeBuAyToplamlariniHesaplar() {
        CariHesap c = cari(1L, 1L);
        LocalDate bugun = LocalDate.now();
        LocalDate gecmis = bugun.minusDays(5);
        LocalDate aySonu = bugun.withDayOfMonth(bugun.lengthOfMonth());
        LocalDate ileri = bugun.plusMonths(2);
        when(taksitRepository.findBySirketIdAndOdemeDurumuNot(1L, "ODENDI"))
                .thenReturn(List.of(
                        kalem(1L, c, gecmis, "100", "BEKLEMEDE"),
                        kalem(2L, c, aySonu, "200", "BEKLEMEDE"),
                        kalem(3L, c, ileri, "300", "BEKLEMEDE")));

        Map<String, Object> ozet = taksitService.ozet(1L);

        BigDecimal beklenenBuAy = new BigDecimal("200");
        if (gecmis.getYear() == bugun.getYear() && gecmis.getMonth() == bugun.getMonth()) {
            beklenenBuAy = beklenenBuAy.add(new BigDecimal("100"));
        }
        assertEquals(new BigDecimal("600"), ozet.get("bekleyenToplam"));
        assertEquals(new BigDecimal("100"), ozet.get("gecikmisToplam"));
        assertEquals(beklenenBuAy, ozet.get("buAyToplam"));
        assertEquals(1, ozet.get("gecikmisAdet"));
    }

    @Test
    void ode_odendiOlarakIsaretler() {
        CariHesap c = cari(1L, 1L);
        Taksit t = kalem(5L, c, LocalDate.now().minusDays(1), "100", "BEKLEMEDE");
        when(taksitRepository.findByIdAndSirketId(5L, 1L)).thenReturn(Optional.of(t));
        when(taksitRepository.save(any(Taksit.class))).thenAnswer(inv -> inv.getArgument(0));

        TaksitDTO sonuc = taksitService.ode(5L, TaksitOdeDTO.builder().hareketId(77L).build(), 1L);

        assertEquals("ODENDI", sonuc.getOdemeDurumu());
        assertEquals(LocalDate.now(), sonuc.getOdemeTarihi());
        assertEquals(77L, sonuc.getHareketId());
    }

    @Test
    void ode_zatenOdenmisseHataFirlatir() {
        CariHesap c = cari(1L, 1L);
        Taksit t = kalem(5L, c, LocalDate.now(), "100", "ODENDI");
        when(taksitRepository.findByIdAndSirketId(5L, 1L)).thenReturn(Optional.of(t));

        assertThrows(BusinessException.class, () -> taksitService.ode(5L, null, 1L));
    }

    @Test
    void ode_bulunamazsaHataFirlatir() {
        when(taksitRepository.findByIdAndSirketId(9L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> taksitService.ode(9L, null, 1L));
    }

    @Test
    void sil_kaydiSiler() {
        CariHesap c = cari(1L, 1L);
        Taksit t = kalem(5L, c, LocalDate.now(), "100", "BEKLEMEDE");
        when(taksitRepository.findByIdAndSirketId(5L, 1L)).thenReturn(Optional.of(t));

        taksitService.sil(5L, 1L);

        verify(taksitRepository).delete(t);
    }

    @Test
    void planSil_planaAitKalemleriSiler() {
        taksitService.planSil("TKS-1", 1L);
        verify(taksitRepository).deleteByPlanNoAndSirketId("TKS-1", 1L);
    }
}
