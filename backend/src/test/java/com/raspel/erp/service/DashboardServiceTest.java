package com.raspel.erp.service;

import com.raspel.erp.dto.HareketDTO;
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
    @InjectMocks private DashboardService dashboardService;

    @Test
    void dashboardVerileriGetir_returnsData() {
        when(cariHesapService.toplamCariSayisiGetir()).thenReturn(10L);
        when(cariHesapService.toplamBakiyeGetir()).thenReturn(BigDecimal.valueOf(100000));
        when(hareketService.sonHareketleriGetir(5)).thenReturn(List.of());
        var result = dashboardService.dashboardVerileriGetir();
        assertEquals(10L, result.getToplamCariSayisi());
        assertEquals(BigDecimal.valueOf(100000), result.getToplamBakiye());
        assertTrue(result.getSonHareketler().isEmpty());
    }

    @Test
    void dashboardFallback_returnsEmpty() {
        var result = dashboardService.dashboardFallback(new RuntimeException("error"));
        assertEquals(0L, result.getToplamCariSayisi());
        assertEquals(BigDecimal.ZERO, result.getToplamBakiye());
        assertTrue(result.getSonHareketler().isEmpty());
    }
}
