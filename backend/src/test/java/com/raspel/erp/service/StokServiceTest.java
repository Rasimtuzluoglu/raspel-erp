package com.raspel.erp.service;

import com.raspel.erp.dto.envanter.StokDTO;
import com.raspel.erp.dto.envanter.StokHareketDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.service.sistem.BildirimService;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.service.envanter.StokService;

@ExtendWith(MockitoExtension.class)
class StokServiceTest {

    @Mock private StokRepository stokRepository;
    @Mock private StokHareketRepository stokHareketRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private BildirimService bildirimService;
    @Mock private TenantChecker tenantChecker;
    @Mock private CacheYardimci cacheYardimci;
    @InjectMocks private StokService stokService;

    private Stok createStok(Long id) {
        Stok s = new Stok();
        s.setId(id);
        s.setStokKodu("STK00" + id);
        s.setAd("Urun " + id);
        s.setBirim("Adet");
        s.setFiyat(BigDecimal.valueOf(100));
        s.setMiktar(BigDecimal.valueOf(50));
        s.setMinMiktar(BigDecimal.valueOf(5));
        s.setOlusturmaTarihi(LocalDateTime.now());
        return s;
    }

    @Test
    void tumunuGetir_returnsAll() {
        Page<Stok> page = new PageImpl<>(List.of(createStok(1L), createStok(2L)));
        when(stokRepository.findBySirketIdOrderByAd(1L, Pageable.unpaged())).thenReturn(page);
        var result = stokService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void ara_returnsFiltered() {
        when(stokRepository.findBySirketIdAndBarkod(1L, "urun")).thenReturn(List.of());
        when(stokRepository.findBySirketIdAndAdContainingIgnoreCase(1L, "urun")).thenReturn(List.of(createStok(1L)));
        var result = stokService.ara("urun", 1L);
        assertEquals(1, result.size());
    }

    @Test
    void getir_returnsStok() {
        when(stokRepository.findById(1L)).thenReturn(Optional.of(createStok(1L)));
        var result = stokService.getir(1L);
        assertEquals("STK001", result.getStokKodu());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(stokRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> stokService.getir(99L));
    }

    @Test
    void olustur_creates() {
        StokDTO dto = StokDTO.builder().stokKodu("STK999").ad("Yeni Urun").birim("KG")
                .fiyat(BigDecimal.valueOf(50)).miktar(BigDecimal.valueOf(100)).minMiktar(BigDecimal.valueOf(10)).build();
        Stok saved = createStok(1L);
        saved.setStokKodu("STK999");
        saved.setAd("Yeni Urun");
        when(stokRepository.save(any(Stok.class))).thenReturn(saved);
        var result = stokService.olustur(dto, 1L);
        assertEquals("STK999", result.getStokKodu());
    }

    @Test
    void guncelle_updates() {
        Stok existing = createStok(1L);
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        StokDTO dto = StokDTO.builder().stokKodu("STK001").ad("Guncel Urun").birim("Adet")
                .fiyat(BigDecimal.valueOf(150)).miktar(BigDecimal.valueOf(200)).minMiktar(BigDecimal.valueOf(20)).build();
        when(stokRepository.save(any(Stok.class))).thenReturn(existing);
        var result = stokService.guncelle(1L, dto);
        assertEquals("Guncel Urun", result.getAd());
    }

    @Test
    void guncelle_miktarDegisinceDuzeltmeHareketiOlusur() {
        Stok existing = createStok(1L);
        existing.setMiktar(BigDecimal.valueOf(50));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        StokDTO dto = StokDTO.builder().stokKodu("STK001").ad("Guncel Urun").birim("Adet")
                .fiyat(BigDecimal.valueOf(150)).miktar(BigDecimal.valueOf(80)).minMiktar(BigDecimal.valueOf(20)).build();
        when(stokRepository.save(any(Stok.class))).thenReturn(existing);

        stokService.guncelle(1L, dto);

        assertEquals(0, existing.getMiktar().compareTo(BigDecimal.valueOf(80)));
        var h = org.mockito.ArgumentCaptor.forClass(StokHareket.class);
        verify(stokHareketRepository).save(h.capture());
        assertEquals("DUZELTME", h.getValue().getTur());
        assertEquals(0, h.getValue().getMiktar().compareTo(BigDecimal.valueOf(30)));
    }

    @Test
    void guncelle_throwsWhenNotFound() {
        when(stokRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> stokService.guncelle(99L, new StokDTO()));
    }

    @Test
    void sil_deletes() {
        Stok s = createStok(1L);
        s.setMiktar(BigDecimal.ZERO);
        when(stokRepository.findById(1L)).thenReturn(Optional.of(s));
        when(stokHareketRepository.countByStokId(1L)).thenReturn(0L);
        stokService.sil(1L);
        verify(stokRepository).deleteById(1L);
    }

    @Test
    void sil_throwsWhenHasMiktar() {
        Stok s = createStok(1L);
        s.setMiktar(BigDecimal.valueOf(10));
        when(stokRepository.findById(1L)).thenReturn(Optional.of(s));
        assertThrows(RuntimeException.class, () -> stokService.sil(1L));
    }

    @Test
    void sil_throwsWhenHasHareket() {
        Stok s = createStok(1L);
        s.setMiktar(BigDecimal.ZERO);
        when(stokRepository.findById(1L)).thenReturn(Optional.of(s));
        when(stokHareketRepository.countByStokId(1L)).thenReturn(2L);
        assertThrows(RuntimeException.class, () -> stokService.sil(1L));
    }

    @Test
    void hareketler_returnsHareketler() {
        Stok stok = createStok(1L);
        StokHareket h = StokHareket.builder().id(1L).stok(stok).tur("GIRIS").miktar(BigDecimal.valueOf(10))
                .hareketTarihi(LocalDate.now()).build();
        when(stokRepository.findById(1L)).thenReturn(Optional.of(stok));
        when(stokHareketRepository.findByStokIdOrderByHareketTarihiDesc(1L)).thenReturn(List.of(h));
        var result = stokService.hareketler(1L);
        assertEquals(1, result.size());
    }

    @Test
    void tumHareketler_returnsAll() {
        Stok stok = createStok(1L);
        when(stokHareketRepository.findByStokSirketIdOrderByHareketTarihiDesc(1L))
                .thenReturn(List.of(StokHareket.builder().id(1L).stok(stok).tur("GIRIS").miktar(BigDecimal.valueOf(5)).hareketTarihi(LocalDate.now()).build()));
        var result = stokService.tumHareketler(1L);
        assertEquals(1, result.size());
    }

    @Test
    void hareketEkle_giris_artirirMiktari() {
        Stok stok = createStok(1L);
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
        StokHareketDTO dto = StokHareketDTO.builder().stokId(1L).tur("GIRIS").miktar(BigDecimal.valueOf(10))
                .hareketTarihi(LocalDate.now()).build();
        StokHareket hareket = StokHareket.builder().id(1L).stok(stok).tur("GIRIS").miktar(BigDecimal.valueOf(10))
                .hareketTarihi(LocalDate.now()).build();
        when(stokHareketRepository.save(any(StokHareket.class))).thenReturn(hareket);
        var result = stokService.hareketEkle(dto);
        assertEquals(BigDecimal.valueOf(60), stok.getMiktar());
        assertNotNull(result);
    }

    @Test
    void hareketEkle_cikis_throwsWhenYetersiz() {
        Stok stok = createStok(1L);
        stok.setMiktar(BigDecimal.valueOf(5));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
        StokHareketDTO dto = StokHareketDTO.builder().stokId(1L).tur("CIKIS").miktar(BigDecimal.valueOf(10))
                .hareketTarihi(LocalDate.now()).build();
        assertThrows(RuntimeException.class, () -> stokService.hareketEkle(dto));
    }

    @Test
    void hareketEkle_cikis_azaltirMiktari() {
        Stok stok = createStok(1L);
        stok.setMiktar(BigDecimal.valueOf(50));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
        StokHareketDTO dto = StokHareketDTO.builder().stokId(1L).tur("CIKIS").miktar(BigDecimal.valueOf(10))
                .hareketTarihi(LocalDate.now()).build();
        StokHareket hareket = StokHareket.builder().id(1L).stok(stok).tur("CIKIS").miktar(BigDecimal.valueOf(10))
                .hareketTarihi(LocalDate.now()).build();
        when(stokHareketRepository.save(any(StokHareket.class))).thenReturn(hareket);
        stokService.hareketEkle(dto);
        assertEquals(BigDecimal.valueOf(40), stok.getMiktar());
    }

    @Test
    void hareketSil_gerialir() {
        Stok stok = createStok(1L);
        stok.setMiktar(BigDecimal.valueOf(50));
        StokHareket h = StokHareket.builder().id(1L).stok(stok).tur("GIRIS").miktar(BigDecimal.valueOf(10))
                .hareketTarihi(LocalDate.now()).build();
        when(stokHareketRepository.findById(1L)).thenReturn(Optional.of(h));
        stokService.hareketSil(1L);
        assertEquals(BigDecimal.valueOf(40), stok.getMiktar());
        verify(stokHareketRepository).deleteById(1L);
    }

    @Test
    void toplamStokAdet_returnsCount() {
        when(stokRepository.countBySirketId(1L)).thenReturn(3L);
        assertEquals(3L, stokService.toplamStokAdet(1L));
    }

    @Test
    void toplamStokMiktari_returnsSum() {
        when(stokRepository.toplamMiktar()).thenReturn(BigDecimal.valueOf(100));
        BigDecimal total = stokService.toplamStokMiktari();
        assertEquals(BigDecimal.valueOf(100), total);
    }

    @Test
    void hareketEkle_kritikSeviyedeBildirimGonderir() {
        Stok stok = createStok(1L);
        stok.setSirketId(1L);
        stok.setMiktar(BigDecimal.valueOf(4));
        stok.setMinMiktar(BigDecimal.valueOf(5));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
        StokHareketDTO dto = StokHareketDTO.builder().stokId(1L).tur("CIKIS").miktar(BigDecimal.valueOf(2))
                .hareketTarihi(LocalDate.now()).build();
        when(stokHareketRepository.save(any(StokHareket.class)))
                .thenReturn(StokHareket.builder().id(1L).stok(stok).tur("CIKIS").miktar(BigDecimal.valueOf(2)).build());
        stokService.hareketEkle(dto);
        verify(bildirimService, times(1)).bildirimGonder(eq(1L), eq("STOK"), anyString(), anyString());
    }

    @Test
    void kritikStoklar_onerilenSiparisMiktariHesaplar() {
        Stok stok = createStok(1L);
        stok.setSirketId(1L);
        stok.setMiktar(BigDecimal.valueOf(5));
        stok.setMinMiktar(BigDecimal.valueOf(10));
        when(stokRepository.kritikStoklar(1L)).thenReturn(List.of(stok));
        var result = stokService.kritikStoklar(1L);
        assertEquals(1, result.size());
        assertEquals(BigDecimal.valueOf(15), result.get(0).getOnerilenSiparisMiktari());
    }

    @Test
    void talepTahmini_calculatesForecastAndSuggestions() {
        Stok stok = createStok(1L);
        stok.setSirketId(1L);
        stok.setMiktar(BigDecimal.valueOf(10));
        stok.setMinMiktar(BigDecimal.valueOf(5));

        when(stokRepository.findBySirketIdOrderByAd(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(stok)));
        when(stokHareketRepository.findByStokIdOrderByHareketTarihiDesc(1L))
                .thenReturn(List.of(
                        StokHareket.builder().tur("CIKIS").miktar(BigDecimal.valueOf(90)).hareketTarihi(LocalDate.now().minusDays(10)).build()
                ));

        var result = stokService.talepTahmini(1L);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getStokId());
        assertTrue(result.get(0).getTahminiTukenmeGunu() > 0);
        assertNotNull(result.get(0).getProaktifOneri());
    }
}