package com.raspel.erp.service.ticaret;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.FaturaGecmis;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ticaret.FaturaGecmisRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaturaGecmisServiceTest {

    @Mock private FaturaGecmisRepository faturaGecmisRepository;
    @Mock private FaturaRepository faturaRepository;
    @Mock private TenantChecker tenantChecker;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private FaturaGecmisService service;

    private Fatura fatura(Long id, String numara) {
        return Fatura.builder()
                .id(id).faturaNumarasi(numara).sirketId(1L)
                .tur(Fatura.FaturaTur.SATIS).durum(Fatura.FaturaDurum.TASLAK)
                .tarih(LocalDate.now()).genelToplam(new BigDecimal("100.00"))
                .kalemler(new java.util.ArrayList<>())
                .build();
    }

    @Test
    void snapshot_temelAlanlariIcerir() {
        Fatura f = fatura(1L, "FTR-1");
        String json = service.snapshot(f);
        assertNotNull(json);
        assertTrue(json.contains("FTR-1"));
        assertTrue(json.contains("kalemSayisi"));
        assertTrue(json.contains("SATIS"));
    }

    @Test
    void diffOzet_degisenAlanlariDoner() {
        Fatura onceki = fatura(1L, "FTR-1");
        Fatura yeni = fatura(1L, "FTR-1");
        yeni.setGenelToplam(new BigDecimal("150.00"));
        yeni.setDurum(Fatura.FaturaDurum.KESILDI);

        String ozet = service.diffOzet(service.snapshot(onceki), service.snapshot(yeni));

        assertNotNull(ozet);
        assertTrue(ozet.contains("genelToplam"));
        assertTrue(ozet.contains("durum"));
    }

    @Test
    void diffOzet_degisiklikYoksaNullDoner() {
        Fatura f = fatura(1L, "FTR-1");
        assertNull(service.diffOzet(service.snapshot(f), service.snapshot(f)));
    }

    @Test
    void kaydet_yazdirmaKopyaNoAtar() {
        Fatura f = fatura(1L, "FTR-1");
        when(faturaGecmisRepository.countByFaturaIdAndOlay(1L, "YAZDIR")).thenReturn(2L);

        service.kaydet(f, FaturaGecmisService.YAZDIR, "Yazdırıldı (A4)", null, null, "A4", "HP Laser");

        ArgumentCaptor<FaturaGecmis> captor = ArgumentCaptor.forClass(FaturaGecmis.class);
        verify(faturaGecmisRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getKopyaNo());
        assertEquals("A4", captor.getValue().getYazdirmaFormat());
        assertEquals("HP Laser", captor.getValue().getYaziciAdi());
        assertEquals(1L, captor.getValue().getSirketId());
    }

    @Test
    void gecmis_tenantKontroluVeDonusum() {
        Fatura f = fatura(1L, "FTR-1");
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(f));
        when(faturaGecmisRepository.findByFaturaIdOrderByTarihDescIdDesc(1L))
                .thenReturn(List.of(FaturaGecmis.builder().id(5L).faturaId(1L).olay("OLUSTUR").build()));

        var sonuc = service.gecmis(1L);

        assertEquals(1, sonuc.size());
        assertEquals("OLUSTUR", sonuc.get(0).getOlay());
        verify(tenantChecker).check(1L, "Fatura");
    }

    @Test
    void yazdirmaKaydet_faturaYoksaHata() {
        when(faturaRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.yazdirmaKaydet(9L, "A4", null));
    }

    @Test
    void yazdirmaKaydet_kaydederVeDoner() {
        Fatura f = fatura(1L, "FTR-1");
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(f));
        when(faturaGecmisRepository.countByFaturaIdAndOlay(1L, "YAZDIR")).thenReturn(0L);
        when(faturaGecmisRepository.findByFaturaIdOrderByTarihDescIdDesc(1L))
                .thenReturn(List.of(FaturaGecmis.builder().id(7L).faturaId(1L).olay("YAZDIR").yazdirmaFormat("TERMAL80").build()));

        var dto = service.yazdirmaKaydet(1L, "termal80", null);

        assertNotNull(dto);
        assertEquals("YAZDIR", dto.getOlay());
        verify(faturaGecmisRepository).save(any(FaturaGecmis.class));
    }

    @Test
    void yazdirmaOzetleri_sayar() {
        FaturaGecmis g1 = FaturaGecmis.builder().id(3L).faturaId(1L).olay("YAZDIR")
                .yazdirmaFormat("A4").build();
        FaturaGecmis g2 = FaturaGecmis.builder().id(1L).faturaId(1L).olay("YAZDIR")
                .yazdirmaFormat("A4").build();
        when(faturaGecmisRepository.findByFaturaIdInAndOlayOrderByTarihDescIdDesc(List.of(1L), "YAZDIR"))
                .thenReturn(List.of(g1, g2));

        var ozet = service.yazdirmaOzetleri(List.of(1L));

        assertEquals(1, ozet.size());
        assertEquals(2, ozet.get(1L).getAdet());
        assertEquals("A4", ozet.get(1L).getSonFormat());
    }

    @Test
    void kaydet_bosListeIcinSessizDoner() {
        service.kaydet(null, FaturaGecmisService.OLUSTUR, "x", null, null);
        verifyNoInteractions(faturaGecmisRepository);
    }

    @Test
    void yazdirmaOzetleri_bosListeBosMapDoner() {
        assertTrue(service.yazdirmaOzetleri(List.of()).isEmpty());
        assertTrue(service.yazdirmaOzetleri(null).isEmpty());
    }

    @Test
    void kaydet_hataDurumundaBloklamaz() {
        Fatura f = fatura(1L, "FTR-1");
        when(faturaGecmisRepository.save(any(FaturaGecmis.class)))
                .thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> service.kaydet(f, FaturaGecmisService.OLUSTUR, "x", null, "{}"));
    }
}
