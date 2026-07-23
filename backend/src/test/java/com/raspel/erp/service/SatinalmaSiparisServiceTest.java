package com.raspel.erp.service;

import com.raspel.erp.dto.SatinalmaSiparisDTO;
import com.raspel.erp.dto.SatinalmaSiparisKalemDTO;
import com.raspel.erp.entity.SatinalmaSiparis;
import com.raspel.erp.repository.*;
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
class SatinalmaSiparisServiceTest {

    @Mock private SatinalmaSiparisRepository siparisRepository;
    @Mock private SatinalmaSiparisKalemRepository kalemRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private StokRepository stokRepository;
    @InjectMocks private SatinalmaSiparisService satinalmaSiparisService;

    private SatinalmaSiparis createSiparis(Long id) {
        SatinalmaSiparis s = new SatinalmaSiparis();
        s.setId(id);
        s.setSiparisNo("SSP-000" + id);
        s.setTarih(LocalDate.now());
        s.setCariHesapId(1L);
        s.setDurum("TASLAK");
        s.setAraToplam(BigDecimal.valueOf(2000));
        s.setKdv(BigDecimal.valueOf(400));
        s.setGenelToplam(BigDecimal.valueOf(2400));
        s.setSirketId(1L);
        s.setOlusturmaTarihi(LocalDateTime.now());
        return s;
    }

    @Test
    void tumunuGetir_returnsAll() {
        when(siparisRepository.findBySirketIdOrderByTarihDesc(1L)).thenReturn(List.of(createSiparis(1L)));
        var result = satinalmaSiparisService.tumunuGetir(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getir_returnsSiparis() {
        when(siparisRepository.findById(1L)).thenReturn(Optional.of(createSiparis(1L)));
        var result = satinalmaSiparisService.getir(1L);
        assertEquals("SSP-0001", result.getSiparisNo());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(siparisRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> satinalmaSiparisService.getir(99L));
    }

    @Test
    void olustur_creates() {
        SatinalmaSiparisKalemDTO kalem = SatinalmaSiparisKalemDTO.builder().stokId(1L)
                .aciklama("K").miktar(BigDecimal.valueOf(5)).birim("Adet").birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20)).tutar(BigDecimal.valueOf(600)).build();
        SatinalmaSiparisDTO dto = SatinalmaSiparisDTO.builder().siparisNo("SSP-999").tarih(LocalDate.now())
                .cariHesapId(1L).aciklama("Test").araToplam(BigDecimal.valueOf(2000)).kdv(BigDecimal.valueOf(400))
                .genelToplam(BigDecimal.valueOf(2400)).sirketId(1L).kalemler(List.of(kalem)).build();
        SatinalmaSiparis saved = createSiparis(1L);
        when(siparisRepository.save(any(SatinalmaSiparis.class))).thenReturn(saved);
        var result = satinalmaSiparisService.olustur(dto);
        assertNotNull(result);
    }

    @Test
    void durumGuncelle_updates() {
        SatinalmaSiparis siparis = createSiparis(1L);
        when(siparisRepository.findById(1L)).thenReturn(Optional.of(siparis));
        when(siparisRepository.save(any(SatinalmaSiparis.class))).thenReturn(siparis);
        var result = satinalmaSiparisService.durumGuncelle(1L, "SIPARIS_VERILDI");
        assertEquals("SIPARIS_VERILDI", result.getDurum());
    }

    @Test
    void durumGuncelle_throwsWhenNotFound() {
        when(siparisRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> satinalmaSiparisService.durumGuncelle(99L, "SIPARIS_VERILDI"));
    }

    @Test
    void sil_deletes() {
        satinalmaSiparisService.sil(1L);
        verify(kalemRepository).deleteBySiparisId(1L);
        verify(siparisRepository).deleteById(1L);
    }
}
