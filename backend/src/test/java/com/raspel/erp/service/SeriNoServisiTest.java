package com.raspel.erp.service;

import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.repository.ticaret.TeklifRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.sistem.SeriNoServisi;

@ExtendWith(MockitoExtension.class)
class SeriNoServisiTest {

    @Mock private FaturaRepository faturaRepository;
    @Mock private SiparisRepository siparisRepository;
    @Mock private TeklifRepository teklifRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @InjectMocks private SeriNoServisi seriNoServisi;

    @BeforeEach
    void setUp() {
        // Sayac satiri yok -> tohum degeri mevcut belgelerden hesaplanir.
        lenient().when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), any(), any()))
                .thenReturn(null);
        // INSERT ... ON CONFLICT RETURNING -> tohum (3. vararg) degerini dondur.
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                .thenAnswer(inv -> {
                    Object[] args = inv.getArguments();
                    Object last = args[args.length - 1];
                    if (last instanceof Object[]) {
                        Object[] va = (Object[]) last;
                        return va[va.length - 1];
                    }
                    return last;
                });
    }

    @Test
    void faturaNoUret_boskenIlkSeri() {
        when(faturaRepository.findFaturaNumarasiByPrefix("FTR-1-2026-", 1L)).thenReturn(List.of());
        assertEquals("FTR-1-2026-000001", seriNoServisi.faturaNoUret(1L));
    }

    @Test
    void faturaNoUret_mevcutSeridenSonraki() {
        when(faturaRepository.findFaturaNumarasiByPrefix("FTR-1-2026-", 1L))
                .thenReturn(List.of("FTR-1-2026-000001", "FTR-1-2026-000003", "FTR-1-2026-000002"));
        assertEquals("FTR-1-2026-000004", seriNoServisi.faturaNoUret(1L));
    }

    @Test
    void faturaNoUret_bozukFormatliKayitlariGormezdenGelir() {
        when(faturaRepository.findFaturaNumarasiByPrefix("FTR-1-2026-", 1L))
                .thenReturn(List.of("FTR-1-2026-000001", "eski-format"));
        assertEquals("FTR-1-2026-000002", seriNoServisi.faturaNoUret(1L));
    }

    @Test
    void siparisNoUret_boskenIlkSeri() {
        when(siparisRepository.findSiparisNoByPrefix("SIP-1-2026-", 1L)).thenReturn(List.of());
        assertEquals("SIP-1-2026-000001", seriNoServisi.siparisNoUret(1L));
    }

    @Test
    void siparisNoUret_mevcutSeridenSonraki() {
        when(siparisRepository.findSiparisNoByPrefix("SIP-1-2026-", 1L))
                .thenReturn(List.of("SIP-1-2026-000007"));
        assertEquals("SIP-1-2026-000008", seriNoServisi.siparisNoUret(1L));
    }
}
