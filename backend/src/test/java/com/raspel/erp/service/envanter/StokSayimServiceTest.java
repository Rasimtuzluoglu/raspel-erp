package com.raspel.erp.service.envanter;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.envanter.StokSayimDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokSayim;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.envanter.StokSayimRepository;
import com.raspel.erp.service.sistem.BildirimService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StokSayimServiceTest {

    @Mock
    private StokSayimRepository stokSayimRepository;
    @Mock
    private StokRepository stokRepository;
    @Mock
    private StokHareketRepository stokHareketRepository;
    @Mock
    private TenantChecker tenantChecker;
    @Mock
    private CacheYardimci cacheYardimci;
    @Mock
    private BildirimService bildirimService;

    @InjectMocks
    private StokSayimService stokSayimService;

    private StokSayim sayim(Long id, BigDecimal beklenen, BigDecimal sayilan) {
        Stok stok = Stok.builder().id(10L).ad("Ürün").sirketId(1L).miktar(new BigDecimal("100")).build();
        return StokSayim.builder()
                .id(id).stok(stok).sirketId(1L)
                .beklenenMiktar(beklenen).sayilanMiktar(sayilan)
                .durum("TASLAK").build();
    }

    @Test
    void getir_bulunamazsaHataFirlatir() {
        when(stokSayimRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> stokSayimService.getir(99L));
    }

    @Test
    void olustur_stokYoksaHataFirlatir() {
        when(stokRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                stokSayimService.olustur(StokSayimDTO.builder().stokId(99L).build(), 1L));
    }

    @Test
    void olustur_varsayilanDegerlerleKaydeder() {
        Stok stok = Stok.builder().id(10L).ad("Ürün").sirketId(1L).build();
        when(stokRepository.findById(10L)).thenReturn(Optional.of(stok));
        StokSayim kaydedilen = sayim(1L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(stokSayimRepository.save(any(StokSayim.class))).thenReturn(kaydedilen);

        StokSayimDTO sonuc = stokSayimService.olustur(StokSayimDTO.builder().stokId(10L).tarih(LocalDate.now()).build(), 1L);

        assertEquals("TASLAK", sonuc.getDurum());
        assertEquals(BigDecimal.ZERO, sonuc.getBeklenenMiktar());
    }

    @Test
    void durumGuncelle_gecersizDurumHataFirlatir() {
        StokSayim s = sayim(1L, new BigDecimal("10"), new BigDecimal("10"));
        when(stokSayimRepository.findById(1L)).thenReturn(Optional.of(s));
        doNothing().when(tenantChecker).check(any(), anyString());

        assertThrows(BusinessException.class, () -> stokSayimService.durumGuncelle(1L, "GECERSIZ"));
    }

    @Test
    void durumGuncelle_tamamlandiFarksizStokDegismez() {
        StokSayim s = sayim(1L, new BigDecimal("10"), new BigDecimal("10"));
        when(stokSayimRepository.findById(1L)).thenReturn(Optional.of(s));
        doNothing().when(tenantChecker).check(any(), anyString());
        when(stokSayimRepository.save(any(StokSayim.class))).thenReturn(s);

        StokSayimDTO sonuc = stokSayimService.durumGuncelle(1L, "TAMAMLANDI");

        assertEquals("TAMAMLANDI", sonuc.getDurum());
        assertEquals(BigDecimal.ZERO, sonuc.getFark());
        verify(stokRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void durumGuncelle_tamamlandiFarkIleStokGunceller() {
        StokSayim s = sayim(1L, new BigDecimal("10"), new BigDecimal("13"));
        Stok stok = Stok.builder().id(10L).ad("Ürün").sirketId(1L).miktar(new BigDecimal("100")).build();
        when(stokSayimRepository.findById(1L)).thenReturn(Optional.of(s));
        doNothing().when(tenantChecker).check(any(), anyString());
        when(stokRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(stok));
        when(stokRepository.save(any(Stok.class))).thenReturn(stok);
        when(stokSayimRepository.save(any(StokSayim.class))).thenReturn(s);

        StokSayimDTO sonuc = stokSayimService.durumGuncelle(1L, "TAMAMLANDI");

        assertEquals("TAMAMLANDI", sonuc.getDurum());
        assertEquals(0, new BigDecimal("3").compareTo(sonuc.getFark()));
        assertEquals(0, new BigDecimal("103").compareTo(stok.getMiktar()));
        verify(stokHareketRepository).save(any());
    }

    @Test
    void sil_bulunamazsaHataFirlatir() {
        when(stokSayimRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> stokSayimService.sil(99L));
    }

    @Test
    void tara_bosBarkodHataFirlatir() {
        assertThrows(BusinessException.class, () -> stokSayimService.tara("", BigDecimal.ONE, 1L));
    }

    @Test
    void tara_stokBulunamazsaHataFirlatir() {
        when(stokRepository.findBySirketIdAndBarkod(1L, "YOK")).thenReturn(java.util.List.of());
        when(stokRepository.findBySirketIdAndStokKodu(1L, "YOK")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> stokSayimService.tara("YOK", BigDecimal.ONE, 1L));
    }

    @Test
    void tara_mevcutTaslakSayilaniArtirir() {
        Stok stok = Stok.builder().id(10L).ad("Ürün").sirketId(1L).miktar(new BigDecimal("100")).build();
        StokSayim sayim = sayim(1L, new BigDecimal("100"), new BigDecimal("3"));
        when(stokRepository.findBySirketIdAndBarkod(1L, "BAR")).thenReturn(java.util.List.of(stok));
        when(stokSayimRepository.findFirstBySirketIdAndStokIdAndDurumOrderByOlusturmaTarihiDesc(1L, 10L, "TASLAK"))
                .thenReturn(Optional.of(sayim));
        when(stokSayimRepository.save(any(StokSayim.class))).thenReturn(sayim);

        StokSayimDTO sonuc = stokSayimService.tara("BAR", new BigDecimal("2"), 1L);

        assertEquals(0, new BigDecimal("5").compareTo(sonuc.getSayilanMiktar()));
    }

    @Test
    void tara_taslakYoksaYeniSayimBaslatir() {
        Stok stok = Stok.builder().id(10L).ad("Ürün").sirketId(1L).miktar(new BigDecimal("100")).build();
        when(stokRepository.findBySirketIdAndBarkod(1L, "BAR")).thenReturn(java.util.List.of(stok));
        when(stokSayimRepository.findFirstBySirketIdAndStokIdAndDurumOrderByOlusturmaTarihiDesc(1L, 10L, "TASLAK"))
                .thenReturn(Optional.empty());
        when(stokSayimRepository.saveAndFlush(any(StokSayim.class))).thenAnswer(i -> i.getArgument(0));
        StokSayim sayim = sayim(2L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(stokSayimRepository.save(any(StokSayim.class))).thenReturn(sayim);

        StokSayimDTO sonuc = stokSayimService.tara("BAR", null, 1L);

        assertNotNull(sonuc);
        assertEquals("TASLAK", sonuc.getDurum());
        verify(stokSayimRepository).saveAndFlush(any(StokSayim.class));
    }
}
