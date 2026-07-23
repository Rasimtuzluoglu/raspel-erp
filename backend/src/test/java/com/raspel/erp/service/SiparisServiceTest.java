package com.raspel.erp.service;

import com.raspel.erp.dto.SiparisDTO;
import com.raspel.erp.dto.SiparisKalemDTO;
import com.raspel.erp.entity.Siparis;
import com.raspel.erp.repository.CariHesapRepository;
import com.raspel.erp.repository.SiparisKalemRepository;
import com.raspel.erp.repository.SiparisRepository;
import com.raspel.erp.repository.StokRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiparisServiceTest {

    @Mock private SiparisRepository siparisRepository;
    @Mock private SiparisKalemRepository kalemRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private StokRepository stokRepository;
    @InjectMocks private SiparisService siparisService;

    private Siparis createSiparis(Long id) {
        Siparis s = new Siparis();
        s.setId(id);
        s.setSiparisNo("SPR-000" + id);
        s.setTarih(LocalDate.now());
        s.setCariHesapId(1L);
        s.setTur("SATIS");
        s.setDurum("TEKLIF");
        s.setAraToplam(BigDecimal.valueOf(1000));
        s.setKdv(BigDecimal.valueOf(200));
        s.setGenelToplam(BigDecimal.valueOf(1200));
        s.setSirketId(1L);
        s.setOlusturmaTarihi(LocalDateTime.now());
        return s;
    }

    @Test
    void tumunuGetir_returnsAll() {
        when(siparisRepository.findBySirketIdOrderByTarihDesc(1L)).thenReturn(List.of(createSiparis(1L)));
        var result = siparisService.tumunuGetir(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getir_returnsSiparis() {
        when(siparisRepository.findById(1L)).thenReturn(Optional.of(createSiparis(1L)));
        var result = siparisService.getir(1L);
        assertEquals("SPR-0001", result.getSiparisNo());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(siparisRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> siparisService.getir(99L));
    }

    @Test
    void olustur_creates() {
        SiparisKalemDTO kalem = SiparisKalemDTO.builder().stokId(1L).aciklama("K").miktar(BigDecimal.valueOf(2))
                .birim("Adet").birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20)).tutar(BigDecimal.valueOf(240)).build();
        SiparisDTO dto = SiparisDTO.builder().siparisNo("SPR-999").tarih(LocalDate.now())
                .cariHesapId(1L).aciklama("Test").araToplam(BigDecimal.valueOf(1000)).kdv(BigDecimal.valueOf(200))
                .genelToplam(BigDecimal.valueOf(1200)).sirketId(1L).kalemler(List.of(kalem)).build();
        Siparis saved = createSiparis(1L);
        when(siparisRepository.save(any(Siparis.class))).thenReturn(saved);
        var result = siparisService.olustur(dto);
        assertNotNull(result);
    }

    @Test
    void durumGuncelle_updates() {
        Siparis siparis = createSiparis(1L);
        when(siparisRepository.findById(1L)).thenReturn(Optional.of(siparis));
        when(siparisRepository.save(any(Siparis.class))).thenReturn(siparis);
        var result = siparisService.durumGuncelle(1L, "ONAYLANDI");
        assertEquals("ONAYLANDI", result.getDurum());
    }

    @Test
    void durumGuncelle_throwsWhenNotFound() {
        when(siparisRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> siparisService.durumGuncelle(99L, "ONAYLANDI"));
    }

    @Test
    void sil_deletes() {
        siparisService.sil(1L);
        verify(kalemRepository).deleteBySiparisId(1L);
        verify(siparisRepository).deleteById(1L);
    }
}
