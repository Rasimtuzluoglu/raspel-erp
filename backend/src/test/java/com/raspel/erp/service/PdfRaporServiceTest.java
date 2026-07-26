package com.raspel.erp.service;

import com.raspel.erp.entity.Irsaliye;
import com.raspel.erp.entity.IrsaliyeKalem;
import com.raspel.erp.entity.Siparis;
import com.raspel.erp.entity.SiparisKalem;
import com.raspel.erp.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfRaporServiceTest {

    @Mock private SiparisRepository siparisRepository;
    @Mock private SiparisKalemRepository siparisKalemRepository;
    @Mock private IrsaliyeRepository irsaliyeRepository;
    @Mock private IrsaliyeKalemRepository irsaliyeKalemRepository;
    @InjectMocks private PdfRaporService pdfRaporService;

    @Test
    void siparisRaporu_returnsPdfBytes() {
        Siparis siparis = new Siparis();
        siparis.setId(1L);
        siparis.setSiparisNo("SPR-001");
        siparis.setTarih(LocalDate.now());
        siparis.setDurum("TEKLIF");
        siparis.setCariHesapId(1L);
        when(siparisRepository.findById(1L)).thenReturn(Optional.of(siparis));
        SiparisKalem kalem = new SiparisKalem();
        kalem.setId(1L);
        kalem.setAciklama("Test Urun");
        kalem.setMiktar(BigDecimal.valueOf(2));
        kalem.setBirimFiyat(BigDecimal.valueOf(100));
        when(siparisKalemRepository.findBySiparisId(1L)).thenReturn(List.of(kalem));
        var result = pdfRaporService.siparisRaporu(1L);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void siparisRaporu_throwsWhenNotFound() {
        when(siparisRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> pdfRaporService.siparisRaporu(99L));
    }

    @Test
    void irsaliyeRaporu_returnsPdfBytes() {
        Irsaliye irsaliye = new Irsaliye();
        irsaliye.setId(1L);
        irsaliye.setIrsaliyeNo("IRS-001");
        irsaliye.setTarih(LocalDate.now());
        irsaliye.setDurum("TASLAK");
        irsaliye.setCariHesapId(1L);
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(irsaliye));
        IrsaliyeKalem kalem = new IrsaliyeKalem();
        kalem.setId(1L);
        kalem.setAciklama("Test Urun");
        kalem.setMiktar(java.math.BigDecimal.valueOf(10));
        when(irsaliyeKalemRepository.findByIrsaliyeId(1L)).thenReturn(List.of(kalem));
        var result = pdfRaporService.irsaliyeRaporu(1L);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void irsaliyeRaporu_throwsWhenNotFound() {
        when(irsaliyeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> pdfRaporService.irsaliyeRaporu(99L));
    }
}
