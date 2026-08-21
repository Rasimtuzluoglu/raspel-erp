package com.raspel.erp.service;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.SatinalmaSiparisDTO;
import com.raspel.erp.dto.ticaret.SatinalmaSiparisKalemDTO;
import com.raspel.erp.entity.ticaret.SatinalmaSiparis;
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
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.SatinalmaSiparisKalemRepository;
import com.raspel.erp.repository.ticaret.SatinalmaSiparisRepository;
import com.raspel.erp.service.ticaret.FaturaService;
import com.raspel.erp.service.ticaret.SatinalmaSiparisService;
import com.raspel.erp.entity.ticaret.Siparis;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.repository.envanter.StokRepository;

@ExtendWith(MockitoExtension.class)
class SatinalmaSiparisServiceTest {

    @Mock private SatinalmaSiparisRepository siparisRepository;
    @Mock private SatinalmaSiparisKalemRepository kalemRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private StokRepository stokRepository;
    @Mock private TenantChecker tenantChecker;
    @Mock private FaturaService faturaService;
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
        when(siparisRepository.findBySirketIdOrderByTarihDesc(1L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(createSiparis(1L))));
        var result = satinalmaSiparisService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, result.getContent().size());
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
        when(siparisRepository.findById(1L)).thenReturn(Optional.of(createSiparis(1L)));
        satinalmaSiparisService.sil(1L);
        verify(kalemRepository).deleteBySiparisId(1L);
        verify(siparisRepository).deleteById(1L);
    }

    @Test
    void sil_throwsWhenFaturalandi() {
        SatinalmaSiparis s = createSiparis(1L);
        s.setDurum("FATURALANDI");
        when(siparisRepository.findById(1L)).thenReturn(Optional.of(s));
        assertThrows(RuntimeException.class, () -> satinalmaSiparisService.sil(1L));
    }

    @Test
    void faturayaCevir_createsAlisFatura() {
        SatinalmaSiparis siparis = createSiparis(1L);
        siparis.setDurum("TESLIM_ALINDI");
        when(siparisRepository.findById(1L)).thenReturn(Optional.of(siparis));
        when(kalemRepository.findBySiparisId(1L)).thenReturn(List.of(
                com.raspel.erp.entity.ticaret.SatinalmaSiparisKalem.builder()
                        .id(1L).siparisId(1L).stokId(1L).aciklama("Ürün").miktar(BigDecimal.valueOf(5))
                        .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20)).build()));
        when(faturaService.faturaOlustur(any(), anyLong(), any(), any()))
                .thenReturn(com.raspel.erp.dto.ticaret.FaturaDTO.builder().id(1L).tur("ALIS").build());
        when(siparisRepository.save(any(SatinalmaSiparis.class))).thenReturn(siparis);

        var result = satinalmaSiparisService.faturayaCevir(1L, 1L, "Yönetici");

        assertNotNull(result);
        assertEquals("ALIS", result.getTur());
        verify(faturaService).faturaOlustur(any(), eq(1L), eq(1L), eq("Yönetici"));
        assertEquals("FATURALANDI", siparis.getDurum());
    }

    @Test
    void faturayaCevir_throwsWhenAlreadyConverted() {
        SatinalmaSiparis siparis = createSiparis(1L);
        siparis.setDurum("FATURALANDI");
        when(siparisRepository.findById(1L)).thenReturn(Optional.of(siparis));
        assertThrows(RuntimeException.class, () -> satinalmaSiparisService.faturayaCevir(1L, 1L, "Yönetici"));
    }
}