package com.raspel.erp.service;

import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.sistem.SeriNoServisi;

@ExtendWith(MockitoExtension.class)
class SeriNoServisiTest {

    @Mock private FaturaRepository faturaRepository;
    @Mock private SiparisRepository siparisRepository;
    @InjectMocks private SeriNoServisi seriNoServisi;

    @Test
    void faturaNoUret_boskenIlkSeri() {
        when(faturaRepository.findFaturaNumarasiByPrefix("FTR-2026-", 1L)).thenReturn(List.of());
        assertEquals("FTR-2026-000001", seriNoServisi.faturaNoUret(1L));
    }

    @Test
    void faturaNoUret_mevcutSeridenSonraki() {
        when(faturaRepository.findFaturaNumarasiByPrefix("FTR-2026-", 1L))
                .thenReturn(List.of("FTR-2026-000001", "FTR-2026-000003", "FTR-2026-000002"));
        assertEquals("FTR-2026-000004", seriNoServisi.faturaNoUret(1L));
    }

    @Test
    void faturaNoUret_bozukFormatliKayitlariGormezdenGelir() {
        when(faturaRepository.findFaturaNumarasiByPrefix("FTR-2026-", 1L))
                .thenReturn(List.of("FTR-2026-000001", "eski-format"));
        assertEquals("FTR-2026-000002", seriNoServisi.faturaNoUret(1L));
    }

    @Test
    void siparisNoUret_boskenIlkSeri() {
        when(siparisRepository.findSiparisNoByPrefix("SIP-2026-", 1L)).thenReturn(List.of());
        assertEquals("SIP-2026-000001", seriNoServisi.siparisNoUret(1L));
    }

    @Test
    void siparisNoUret_mevcutSeridenSonraki() {
        when(siparisRepository.findSiparisNoByPrefix("SIP-2026-", 1L))
                .thenReturn(List.of("SIP-2026-000007"));
        assertEquals("SIP-2026-000008", seriNoServisi.siparisNoUret(1L));
    }
}