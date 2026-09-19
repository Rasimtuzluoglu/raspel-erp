package com.raspel.erp.service.ticaret;

import com.raspel.erp.entity.ticaret.Teslimat;
import com.raspel.erp.service.sistem.BildirimService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeslimatGecikmeUyarisiServiceTest {

    @Mock private TeslimatService teslimatService;
    @Mock private BildirimService bildirimService;
    @InjectMocks private TeslimatGecikmeUyarisiService service;

    @Test
    void gecikmisTeslimatlariBildir_herBiriIcinBildirimVeIsaretlemeYapar() {
        Teslimat t1 = Teslimat.builder().id(1L).sirketId(10L).musteriAdi("A Market")
                .beklenenTeslimTarihi(LocalDate.now().minusDays(2)).build();
        Teslimat t2 = Teslimat.builder().id(2L).sirketId(10L).faturaNumarasi("FTR-2")
                .beklenenTeslimTarihi(LocalDate.now().minusDays(1)).build();
        when(teslimatService.gecikmisTeslimatlar()).thenReturn(List.of(t1, t2));

        service.gecikmisTeslimatlariBildir();

        verify(bildirimService, times(2)).bildirimGonder(eq(10L), eq("TESLIMAT"), anyString(), anyString());
        verify(teslimatService).gecikmeBildirildiIsaretle(1L);
        verify(teslimatService).gecikmeBildirildiIsaretle(2L);
    }

    @Test
    void gecikmisTeslimatlariBildir_sirketIdYoksaBildirimGondermezAmaIsaretler() {
        Teslimat t = Teslimat.builder().id(5L).sirketId(null).build();
        when(teslimatService.gecikmisTeslimatlar()).thenReturn(List.of(t));

        service.gecikmisTeslimatlariBildir();

        verifyNoInteractions(bildirimService);
        verify(teslimatService).gecikmeBildirildiIsaretle(5L);
    }

    @Test
    void gecikmisTeslimatlariBildir_bildirimHatasiDonguyuDurdurmaz() {
        Teslimat t1 = Teslimat.builder().id(1L).sirketId(10L).build();
        Teslimat t2 = Teslimat.builder().id(2L).sirketId(10L).build();
        when(teslimatService.gecikmisTeslimatlar()).thenReturn(List.of(t1, t2));
        doThrow(new RuntimeException("patladı")).when(bildirimService)
                .bildirimGonder(anyLong(), anyString(), anyString(), anyString());

        // Istisna disari sizmamali; bildirim basarisizsa isaretleme yapilmaz.
        service.gecikmisTeslimatlariBildir();

        verify(bildirimService, times(2)).bildirimGonder(anyLong(), anyString(), anyString(), anyString());
        verify(teslimatService, never()).gecikmeBildirildiIsaretle(any());
    }

    @Test
    void gecikmisTeslimatlariBildir_listeBosIseBildirimYok() {
        when(teslimatService.gecikmisTeslimatlar()).thenReturn(List.of());

        service.gecikmisTeslimatlariBildir();

        verifyNoInteractions(bildirimService);
        verify(teslimatService, never()).gecikmeBildirildiIsaretle(any());
    }
}
