package com.raspel.erp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.dto.sistem.DashboardTercihDTO;
import com.raspel.erp.entity.sistem.DashboardTercih;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.DashboardTercihRepository;
import com.raspel.erp.service.sistem.DashboardTercihService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Anasayfa düzeni (tercih): upsert, tenant izolasyonu, JSON doğrulama ve
 * 20 KB boyut sınırı.
 */
@ExtendWith(MockitoExtension.class)
class DashboardTercihTest {

    @Mock
    private DashboardTercihRepository dashboardTercihRepository;

    private DashboardTercihService servis;

    @BeforeEach
    void kur() {
        servis = new DashboardTercihService(dashboardTercihRepository, new ObjectMapper());
    }

    private Map<String, Object> ornekDuzen() {
        Map<String, Object> duzen = new LinkedHashMap<>();
        duzen.put("versiyon", 1);
        duzen.put("sira", List.of(Map.of("key", "kpi", "boyut", 2, "gorunur", true)));
        return duzen;
    }

    @Test
    void tercihYoksaDuzenNullDoner() {
        when(dashboardTercihRepository.findByKullaniciIdAndSirketId(5L, 1L)).thenReturn(Optional.empty());

        DashboardTercihDTO dto = servis.tercihGetir(5L, 1L);

        assertNull(dto.getDuzen());
        assertNull(dto.getGuncellemeTarihi());
    }

    @Test
    void tercihKaydet_yoksaOlustururVeJsonSaklar() {
        when(dashboardTercihRepository.findByKullaniciId(5L)).thenReturn(Optional.empty());
        when(dashboardTercihRepository.save(any(DashboardTercih.class))).thenAnswer(inv -> {
            DashboardTercih t = inv.getArgument(0);
            t.setId(11L);
            return t;
        });

        DashboardTercihDTO dto = servis.tercihKaydet(5L, 1L,
                DashboardTercihDTO.builder().duzen(ornekDuzen()).build());

        assertEquals(1, dto.getDuzen().get("versiyon"));
        assertNotNull(dto.getGuncellemeTarihi());
        verify(dashboardTercihRepository).save(argThat(t ->
                t.getKullaniciId().equals(5L) && t.getSirketId().equals(1L)
                        && t.getDuzen().startsWith("{") && t.getDuzen().contains("\"versiyon\"")));
    }

    @Test
    void tercihKaydet_varsaGunceller() {
        DashboardTercih mevcut = DashboardTercih.builder()
                .id(3L).kullaniciId(5L).sirketId(1L).duzen("{\"versiyon\":1}")
                .guncellemeTarihi(LocalDateTime.now().minusDays(1))
                .build();
        when(dashboardTercihRepository.findByKullaniciId(5L)).thenReturn(Optional.of(mevcut));
        when(dashboardTercihRepository.save(any(DashboardTercih.class))).thenAnswer(inv -> inv.getArgument(0));

        DashboardTercihDTO dto = servis.tercihKaydet(5L, 1L,
                DashboardTercihDTO.builder().duzen(ornekDuzen()).build());

        assertEquals(1, dto.getDuzen().get("versiyon"));
        verify(dashboardTercihRepository).save(argThat(t -> t.getId().equals(3L) && t.getDuzen().contains("sira")));
    }

    @Test
    void tercihKaydet_baskaSirketinKaydiniReddeder() {
        DashboardTercih mevcut = DashboardTercih.builder()
                .id(3L).kullaniciId(5L).sirketId(2L).duzen("{\"versiyon\":1}")
                .build();
        when(dashboardTercihRepository.findByKullaniciId(5L)).thenReturn(Optional.of(mevcut));

        BusinessException hata = assertThrows(BusinessException.class, () ->
                servis.tercihKaydet(5L, 1L, DashboardTercihDTO.builder().duzen(ornekDuzen()).build()));

        assertTrue(hata.getMessage().contains("şirkete ait"));
        verify(dashboardTercihRepository, never()).save(any());
    }

    @Test
    void tercihGetir_baskaSirketinKaydiniDondurmez() {
        when(dashboardTercihRepository.findByKullaniciIdAndSirketId(5L, 1L)).thenReturn(Optional.empty());

        assertNull(servis.tercihGetir(5L, 1L).getDuzen());
    }

    @Test
    void tercihKaydet_duzenBosIseHataDoner() {
        assertThrows(BusinessException.class, () -> servis.tercihKaydet(5L, 1L, DashboardTercihDTO.builder().build()));
        assertThrows(BusinessException.class, () -> servis.tercihKaydet(5L, 1L, null));
        verify(dashboardTercihRepository, never()).save(any());
    }

    @Test
    void tercihKaydet_kullaniciVeyaSirketYoksaHataDoner() {
        assertThrows(BusinessException.class, () -> servis.tercihGetir(null, 1L));
        assertThrows(BusinessException.class, () -> servis.tercihGetir(5L, null));
        verifyNoInteractions(dashboardTercihRepository);
    }

    @Test
    void tercihKaydet_20KbUzeriniReddeder() {
        Map<String, Object> buyuk = new LinkedHashMap<>();
        buyuk.put("veri", "x".repeat(21 * 1024));

        BusinessException hata = assertThrows(BusinessException.class, () ->
                servis.tercihKaydet(5L, 1L, DashboardTercihDTO.builder().duzen(buyuk).build()));

        assertTrue(hata.getMessage().contains("20 KB"));
        verify(dashboardTercihRepository, never()).save(any());
    }

    @Test
    void tercihGetir_bozukJsonVarsaNullDoner() {
        DashboardTercih bozuk = DashboardTercih.builder()
                .id(3L).kullaniciId(5L).sirketId(1L).duzen("{bozuk")
                .guncellemeTarihi(LocalDateTime.now())
                .build();
        when(dashboardTercihRepository.findByKullaniciIdAndSirketId(5L, 1L)).thenReturn(Optional.of(bozuk));

        DashboardTercihDTO dto = servis.tercihGetir(5L, 1L);

        assertNull(dto.getDuzen());
        assertNotNull(dto.getGuncellemeTarihi());
    }
}
