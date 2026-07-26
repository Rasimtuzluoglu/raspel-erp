package com.raspel.erp.service;

import com.raspel.erp.dto.HareketDTO;
import com.raspel.erp.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    @InjectMocks private DashboardService dashboardService;

    @Test
    void dashboardVerileriGetir_returnsData() {
        when(cariHesapService.toplamCariSayisiGetir(1L)).thenReturn(10L);
        when(cariHesapService.toplamBakiyeGetir(1L)).thenReturn(BigDecimal.valueOf(100000));
        when(hareketService.sonHareketleriGetir(5)).thenReturn(List.of());
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
