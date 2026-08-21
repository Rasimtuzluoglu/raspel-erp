package com.raspel.erp.service;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.*;
import com.raspel.erp.entity.ticaret.Teklif;
import com.raspel.erp.entity.ticaret.TeklifKalem;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.ticaret.TeklifKalemRepository;
import com.raspel.erp.repository.ticaret.TeklifRepository;
import com.raspel.erp.service.sistem.BildirimService;
import com.raspel.erp.service.sistem.SeriNoServisi;
import com.raspel.erp.service.ticaret.FaturaService;
import com.raspel.erp.service.ticaret.SiparisService;
import com.raspel.erp.service.ticaret.TeklifService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeklifServiceTest {

    @Mock private TeklifRepository teklifRepository;
    @Mock private TeklifKalemRepository kalemRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private StokRepository stokRepository;
    @Mock private SiparisService siparisService;
    @Mock private FaturaService faturaService;
    @Mock private SeriNoServisi seriNoServisi;
    @Mock private BildirimService bildirimService;
    @Mock private TenantChecker tenantChecker;
    @InjectMocks private TeklifService teklifService;

    private Teklif createTeklif(Long id) {
        Teklif t = new Teklif();
        t.setId(id);
        t.setTeklifNo("TKL-2026-000" + id);
        t.setRevizyonNo(0);
        t.setTarih(LocalDate.now());
        t.setCariHesapId(1L);
        t.setTur("SATIS");
        t.setDurum("TASLAK");
        t.setAraToplam(BigDecimal.valueOf(1000));
        t.setKdv(BigDecimal.valueOf(200));
        t.setGenelToplam(BigDecimal.valueOf(1200));
        t.setSirketId(1L);
        t.setOlusturmaTarihi(LocalDateTime.now());
        return t;
    }

    @Test
    void shouldFindAllTeklifler() {
        when(teklifRepository.findBySirketIdOrderByTarihDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createTeklif(1L))));

        var result = teklifService.tumunuGetir(1L, Pageable.unpaged());
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void shouldGetTeklifById() {
        Teklif t = createTeklif(1L);
        when(teklifRepository.findById(1L)).thenReturn(Optional.of(t));

        TeklifDTO dto = teklifService.getir(1L);
        assertNotNull(dto);
        assertEquals("TKL-2026-0001", dto.getTeklifNo());
    }

    @Test
    void shouldCreateTeklif() {
        when(seriNoServisi.teklifNoUret(1L)).thenReturn("TKL-2026-0001");
        when(teklifRepository.save(any(Teklif.class))).thenAnswer(inv -> {
            Teklif arg = inv.getArgument(0);
            arg.setId(10L);
            return arg;
        });

        TeklifKalemDTO kDto = TeklifKalemDTO.builder()
                .aciklama("Test Ürün")
                .miktar(BigDecimal.valueOf(2))
                .birimFiyat(BigDecimal.valueOf(500))
                .kdvOrani(BigDecimal.valueOf(20))
                .build();

        TeklifDTO dto = TeklifDTO.builder()
                .cariHesapId(1L)
                .tarih(LocalDate.now())
                .kalemler(List.of(kDto))
                .build();

        TeklifDTO result = teklifService.olustur(dto, 1L);
        assertNotNull(result);
        assertEquals("TKL-2026-0001", result.getTeklifNo());
        verify(kalemRepository, times(1)).save(any(TeklifKalem.class));
    }

    @Test
    void shouldCreateRevision() {
        Teklif eski = createTeklif(1L);
        when(teklifRepository.findById(1L)).thenReturn(Optional.of(eski));
        when(kalemRepository.findByTeklifId(1L)).thenReturn(List.of(
                TeklifKalem.builder().teklifId(1L).aciklama("Kalem 1").miktar(BigDecimal.ONE).birimFiyat(BigDecimal.TEN).build()
        ));
        when(teklifRepository.save(any(Teklif.class))).thenAnswer(inv -> inv.getArgument(0));

        TeklifDTO rev = teklifService.revizyonOlustur(1L);
        assertNotNull(rev);
        assertEquals(1, rev.getRevizyonNo());
        assertEquals("TASLAK", rev.getDurum());
    }

    @Test
    void shouldConvertToSiparis() {
        Teklif t = createTeklif(1L);
        when(teklifRepository.findById(1L)).thenReturn(Optional.of(t));
        when(kalemRepository.findByTeklifId(1L)).thenReturn(List.of());
        when(siparisService.olustur(any(SiparisDTO.class), eq(1L))).thenReturn(
                SiparisDTO.builder().id(50L).siparisNo("SIP-001").build()
        );

        SiparisDTO sip = teklifService.sipariseDonustur(1L);
        assertNotNull(sip);
        assertEquals("SIPARISE_DONUSTU", t.getDurum());
        verify(teklifRepository).save(t);
    }
}
