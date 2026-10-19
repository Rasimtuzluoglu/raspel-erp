package com.raspel.erp.service.envanter;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.envanter.StokSeriDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokSeri;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.envanter.StokSeriRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StokSeriServiceTest {

    @Mock
    private StokSeriRepository stokSeriRepository;
    @Mock
    private StokRepository stokRepository;
    @Mock
    private com.raspel.erp.repository.envanter.StokHareketRepository stokHareketRepository;
    @Mock
    private TenantChecker tenantChecker;

    @InjectMocks
    private StokSeriService stokSeriService;

    private StokSeri seri(Long id, Long stokId) {
        Stok stok = Stok.builder().id(stokId).ad("Ürün").sirketId(1L).build();
        return StokSeri.builder().id(id).stok(stok).seriNo("SN" + id).durum("STOKTA").build();
    }

    @Test
    void tumunuGetir_nullSirketIdBosSayfaDoner() {
        Page<StokSeriDTO> sonuc = stokSeriService.tumunuGetir(null, Pageable.unpaged());
        assertTrue(sonuc.isEmpty());
    }

    @Test
    void filtreliGetir_sayfaliDonerVeDTOyaMapEder() {
        StokSeri s = seri(1L, 10L);
        s.setDurum("STOKTA");
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 25);
        when(stokSeriRepository.filtreliGetir(eq(1L), any(), eq("STOKTA"), any(), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(s)));

        Page<StokSeriDTO> sonuc = stokSeriService.filtreliGetir(1L, "SN", "STOKTA", null, null, null, pageable);

        assertEquals(1, sonuc.getTotalElements());
        assertEquals("SN1", sonuc.getContent().get(0).getSeriNo());
    }

    @Test
    void getir_bulunamazsaHataFirlatir() {
        when(stokSeriRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> stokSeriService.getir(99L));
    }

    @Test
    void getir_basariliDonus() {
        when(stokSeriRepository.findById(1L)).thenReturn(Optional.of(seri(1L, 10L)));
        doNothing().when(tenantChecker).check(any(), anyString());

        StokSeriDTO sonuc = stokSeriService.getir(1L);

        assertEquals("SN1", sonuc.getSeriNo());
        assertEquals(10L, sonuc.getStokId());
        verify(tenantChecker).check(eq(1L), eq("StokSeri"));
    }

    @Test
    void olustur_stokYoksaHataFirlatir() {
        when(stokRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                stokSeriService.olustur(StokSeriDTO.builder().stokId(99L).seriNo("X").build()));
    }

    @Test
    void olustur_basariliKayit() {
        Stok stok = Stok.builder().id(10L).ad("Ürün").sirketId(1L).build();
        when(stokRepository.findById(10L)).thenReturn(Optional.of(stok));
        StokSeri kaydedilen = seri(1L, 10L);
        kaydedilen.setSeriNo("SN-NEW");
        when(stokSeriRepository.save(any(StokSeri.class))).thenReturn(kaydedilen);

        StokSeriDTO sonuc = stokSeriService.olustur(StokSeriDTO.builder().stokId(10L).seriNo("SN-NEW").build());

        assertEquals("SN-NEW", sonuc.getSeriNo());
    }

    @Test
    void sil_bulunamazsaHataFirlatir() {
        when(stokSeriRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> stokSeriService.sil(99L));
    }

    @Test
    void sil_basarili() {
        when(stokSeriRepository.findById(1L)).thenReturn(Optional.of(seri(1L, 10L)));
        doNothing().when(tenantChecker).check(any(), anyString());

        stokSeriService.sil(1L);

        verify(stokSeriRepository).deleteById(1L);
    }

    @Test
    void guncelle_seriNoGunceller() {
        StokSeri mevcut = seri(1L, 10L);
        when(stokSeriRepository.findById(1L)).thenReturn(Optional.of(mevcut));
        doNothing().when(tenantChecker).check(any(), anyString());
        when(stokSeriRepository.save(any(StokSeri.class))).thenReturn(mevcut);

        StokSeriDTO sonuc = stokSeriService.guncelle(1L, StokSeriDTO.builder().seriNo("SN-GUNCELLE").build());

        assertEquals("SN-GUNCELLE", sonuc.getSeriNo());
    }

    @Test
    void fefoTuket_sktYakinOnceTukenir() {
        StokSeri yakin = StokSeri.builder().id(1L).seriNo("A")
                .sonKullanmaTarihi(java.time.LocalDate.now().plusDays(5))
                .kalanMiktar(new java.math.BigDecimal("3")).durum("STOKTA").build();
        StokSeri uzak = StokSeri.builder().id(2L).seriNo("B")
                .sonKullanmaTarihi(java.time.LocalDate.now().plusDays(30))
                .kalanMiktar(new java.math.BigDecimal("10")).durum("STOKTA").build();
        when(stokSeriRepository.fefoUygun(10L, 5L)).thenReturn(java.util.List.of(yakin, uzak));
        when(stokSeriRepository.save(any(StokSeri.class))).thenAnswer(inv -> inv.getArgument(0));

        var tuketilen = stokSeriService.fefoTuket(10L, 5L, new java.math.BigDecimal("5"));

        assertEquals(0, yakin.getKalanMiktar().compareTo(java.math.BigDecimal.ZERO));
        assertEquals("TUKETILDI", yakin.getDurum());
        assertEquals(0, uzak.getKalanMiktar().compareTo(new java.math.BigDecimal("8")));
        assertEquals(2, tuketilen.size());
    }

    @Test
    void olustur_mukerrerSeriNoReddedilir() {
        Stok stok = Stok.builder().id(10L).ad("Ürün").sirketId(1L).build();
        when(stokRepository.findById(10L)).thenReturn(Optional.of(stok));
        when(stokSeriRepository.existsByStokIdAndSeriNo(10L, "SN-X")).thenReturn(true);

        assertThrows(com.raspel.erp.exception.BusinessException.class, () ->
                stokSeriService.olustur(StokSeriDTO.builder().stokId(10L).seriNo("SN-X").build()));
        verify(stokSeriRepository, never()).save(any());
    }

    @Test
    void guncelle_tuketilmisReddedilir() {
        StokSeri s = seri(1L, 10L);
        s.setDurum("TUKETILDI");
        when(stokSeriRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThrows(com.raspel.erp.exception.BusinessException.class, () ->
                stokSeriService.guncelle(1L, StokSeriDTO.builder().seriNo("YENI").build()));
        verify(stokSeriRepository, never()).save(any());
    }

    @Test
    void sil_tuketilmisReddedilir() {
        StokSeri s = seri(1L, 10L);
        s.setDurum("TUKETILDI");
        when(stokSeriRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> stokSeriService.sil(1L));
        verify(stokSeriRepository, never()).deleteById(1L);
    }

    @Test
    void sil_hareketeBagliReddedilir() {
        when(stokSeriRepository.findById(1L)).thenReturn(Optional.of(seri(1L, 10L)));
        when(stokHareketRepository.existsBySeriId(1L)).thenReturn(true);

        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> stokSeriService.sil(1L));
        verify(stokSeriRepository, never()).deleteById(1L);
    }
}
