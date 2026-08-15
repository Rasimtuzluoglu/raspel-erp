package com.raspel.erp.service;

import com.raspel.erp.dto.finans.HareketDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.service.sistem.DashboardService;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.service.finans.HareketService;
import com.raspel.erp.repository.ik.PersonelIzinRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private CariHesapService cariHesapService;
    @Mock private HareketService hareketService;
    @Mock private HareketRepository hareketRepository;
    @Mock private SiparisRepository siparisRepository;
    @Mock private PersonelRepository personelRepository;
    @Mock private PersonelIzinRepository personelIzinRepository;
    @Mock private StokHareketRepository stokHareketRepository;
    @Mock private StokRepository stokRepository;
    @Mock private FaturaRepository faturaRepository;
    @InjectMocks private DashboardService dashboardService;

    @Test
    void dashboardVerileriGetir_returnsData() {
        when(cariHesapService.toplamCariSayisiGetir(1L)).thenReturn(10L);
        when(cariHesapService.toplamBakiyeGetir(1L)).thenReturn(BigDecimal.valueOf(100000));
        when(hareketService.sonHareketleriGetir(5, 1L)).thenReturn(List.of());
        when(faturaRepository.findVadesiGecen(any(), any(), any(), any())).thenReturn(List.of());
        when(faturaRepository.findVadesiYaklasan(any(), any(), any(), any(), any())).thenReturn(List.of());
        var result = dashboardService.dashboardVerileriGetir(1L);
        assertEquals(10L, result.getToplamCariSayisi());
        assertEquals(BigDecimal.valueOf(100000), result.getToplamBakiye());
        assertTrue(result.getSonHareketler().isEmpty());
    }

    @Test
    void dashboard_returnsDefaultsOnError() {
        when(cariHesapService.toplamCariSayisiGetir(1L)).thenThrow(new RuntimeException("DB error"));
        var result = dashboardService.dashboardVerileriGetir(1L);
        assertNotNull(result);
        assertEquals(0L, result.getToplamCariSayisi());
    }
}