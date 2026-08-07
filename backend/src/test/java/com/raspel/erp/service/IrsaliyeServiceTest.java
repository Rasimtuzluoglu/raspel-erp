package com.raspel.erp.service;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.muhasebe.IrsaliyeDTO;
import com.raspel.erp.dto.muhasebe.IrsaliyeKalemDTO;
import com.raspel.erp.entity.muhasebe.Irsaliye;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeKalemRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeRepository;
import com.raspel.erp.service.muhasebe.IrsaliyeService;
import com.raspel.erp.repository.envanter.StokRepository;

@ExtendWith(MockitoExtension.class)
class IrsaliyeServiceTest {

    @Mock private IrsaliyeRepository irsaliyeRepository;
    @Mock private IrsaliyeKalemRepository kalemRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private StokRepository stokRepository;
    @Mock private TenantChecker tenantChecker;
    @InjectMocks private IrsaliyeService irsaliyeService;

    private Irsaliye createIrsaliye(Long id) {
        Irsaliye i = new Irsaliye();
        i.setId(id);
        i.setIrsaliyeNo("IRS-000" + id);
        i.setTarih(LocalDate.now());
        i.setCariHesapId(1L);
        i.setDurum("TASLAK");
        i.setTur("SATIS");
        i.setSirketId(1L);
        i.setOlusturmaTarihi(LocalDateTime.now());
        return i;
    }

    @Test
    void tumunuGetir_returnsAll() {
        when(irsaliyeRepository.findBySirketIdOrderByTarihDesc(1L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(createIrsaliye(1L))));
        var result = irsaliyeService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getir_returnsIrsaliye() {
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(createIrsaliye(1L)));
        var result = irsaliyeService.getir(1L);
        assertEquals("IRS-0001", result.getIrsaliyeNo());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(irsaliyeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> irsaliyeService.getir(99L));
    }

    @Test
    void olustur_creates() {
        IrsaliyeKalemDTO kalem = IrsaliyeKalemDTO.builder().stokId(1L).aciklama("K").miktar(java.math.BigDecimal.valueOf(5)).birim("Adet").build();
        IrsaliyeDTO dto = IrsaliyeDTO.builder().irsaliyeNo("IRS-999").tarih(LocalDate.now())
                .cariHesapId(1L).aciklama("Test").tur("SATIS").sirketId(1L).kalemler(List.of(kalem)).build();
        Irsaliye saved = createIrsaliye(1L);
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenReturn(saved);
        var result = irsaliyeService.olustur(dto);
        assertNotNull(result);
    }

    @Test
    void durumGuncelle_updates() {
        Irsaliye irsaliye = createIrsaliye(1L);
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(irsaliye));
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenReturn(irsaliye);
        var result = irsaliyeService.durumGuncelle(1L, "SEVK_EDILDI");
        assertEquals("SEVK_EDILDI", result.getDurum());
    }

    @Test
    void durumGuncelle_throwsWhenNotFound() {
        when(irsaliyeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> irsaliyeService.durumGuncelle(99L, "SEVK_EDILDI"));
    }

    @Test
    void sil_deletes() {
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(createIrsaliye(1L)));
        irsaliyeService.sil(1L);
        verify(kalemRepository).deleteByIrsaliyeId(1L);
        verify(irsaliyeRepository).deleteById(1L);
    }
}