package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.FaturaDTO;
import com.raspel.erp.dto.ticaret.EFaturaDTO;
import com.raspel.erp.entity.ticaret.EFatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.ticaret.EFaturaRepository;
import com.raspel.erp.service.ticaret.FaturaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.sistem.SirketRepository;

@ExtendWith(MockitoExtension.class)
class EFaturaServiceTest {

    @Mock
    private EFaturaRepository eFaturaRepository;

    @Mock
    private FaturaService faturaService;

    @Mock
    private com.raspel.erp.repository.finans.CariHesapRepository cariHesapRepository;

    @Mock
    private com.raspel.erp.repository.sistem.SirketRepository sirketRepository;

    @Mock
    private TenantChecker tenantChecker;

    @Mock
    private org.springframework.web.client.RestTemplate restTemplate;

    @InjectMocks
    private EFaturaService eFaturaService;

    private FaturaDTO mockFatura;
    private EFatura mockEFatura;

    @BeforeEach
    void setUp() {
        mockFatura = FaturaDTO.builder()
                .id(1L)
                .faturaNumarasi("FTR-2026-00001")
                .genelToplam(BigDecimal.valueOf(1500.00))
                .cariHesapAd("Test Müşteri Ltd.")
                .tarih(java.time.LocalDate.now())
                .build();

        mockEFatura = EFatura.builder()
                .id(10L)
                .faturaId(1L)
                .ettn("123e4567-e89b-12d3-a456-426614174000")
                .faturaNo("FTR-2026-00001")
                .gibDurumKodu(1000)
                .gibDurumAciklama("Hazırlandı")
                .ublXml("<xml>Test</xml>")
                .sirketId(100L)
                .build();
    }

    @Test
    void testEFaturaOlustur_Basarili() {
        when(faturaService.faturaGetir(1L)).thenReturn(mockFatura);
        when(eFaturaRepository.findByFaturaId(1L)).thenReturn(Optional.empty());
        when(eFaturaRepository.save(any(EFatura.class))).thenReturn(mockEFatura);

        EFaturaDTO result = eFaturaService.eFaturaOlustur(1L, "TEMELFATURA", "SATIS", 100L);

        assertNotNull(result);
        assertEquals("FTR-2026-00001", result.getFaturaNo());
        assertEquals(1000, result.getGibDurumKodu());
        verify(eFaturaRepository, times(1)).save(any(EFatura.class));
    }

    @Test
    void testEFaturaOlustur_ZatenVarIseHataVerir() {
        when(faturaService.faturaGetir(1L)).thenReturn(mockFatura);
        when(eFaturaRepository.findByFaturaId(1L)).thenReturn(Optional.of(mockEFatura));

        assertThrows(BusinessException.class, () -> eFaturaService.eFaturaOlustur(1L, "TEMELFATURA", "SATIS", 100L));
    }

    @Test
    void testGibGonder_EndpointYoksaIletilemez() {
        when(eFaturaRepository.findById(10L)).thenReturn(Optional.of(mockEFatura));

        assertThrows(BusinessException.class, () -> eFaturaService.gibGonder(10L));
        assertEquals(1000, mockEFatura.getGibDurumKodu());
        verify(eFaturaRepository, never()).save(any());
    }

    @Test
    void testGibGonder_EndpointBasariliIseIletilir() throws Exception {
        String endpoint = "https://gib-test.example/gib";
        var field = EFaturaService.class.getDeclaredField("gibEndpoint");
        field.setAccessible(true);
        field.set(eFaturaService, endpoint);
        when(eFaturaRepository.findById(10L)).thenReturn(Optional.of(mockEFatura));
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok().build());
        when(eFaturaRepository.save(any(EFatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EFaturaDTO result = eFaturaService.gibGonder(10L);

        assertNotNull(result);
        assertEquals(1200, result.getGibDurumKodu());
        verify(eFaturaRepository, times(1)).save(mockEFatura);
    }

    @Test
    void testGibGonder_EndpointHataVerirseIletildiIsaretlenmez() throws Exception {
        String endpoint = "https://gib-test.example/gib";
        var field = EFaturaService.class.getDeclaredField("gibEndpoint");
        field.setAccessible(true);
        field.set(eFaturaService, endpoint);
        when(eFaturaRepository.findById(10L)).thenReturn(Optional.of(mockEFatura));
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new org.springframework.web.client.ResourceAccessException("baglanti yok"));

        assertThrows(BusinessException.class, () -> eFaturaService.gibGonder(10L));
        assertEquals(1000, mockEFatura.getGibDurumKodu());
        verify(eFaturaRepository, never()).save(any());
    }
}