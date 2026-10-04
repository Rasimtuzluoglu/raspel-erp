package com.raspel.erp.service;

import com.raspel.erp.dto.envanter.StokDTO;
import com.raspel.erp.dto.envanter.StokHareketDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.exception.BusinessException;
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
import java.util.Set;

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
    @Mock private com.raspel.erp.repository.envanter.StokFiyatRepository stokFiyatRepository;
    @Mock private com.raspel.erp.service.sube.DepoStokService depoStokService;
    @Mock private com.raspel.erp.repository.sube.DepoRepository depoRepository;
    @Mock private com.raspel.erp.repository.envanter.StokSeriRepository stokSeriRepository;
    @Mock private com.raspel.erp.service.envanter.MaliyetService maliyetService;
    @Mock private com.raspel.erp.service.envanter.BarkodUretService barkodUretService;
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
        when(barkodUretService.ean13Uret(1L)).thenReturn("8690000000005");
        when(stokRepository.save(any(Stok.class))).thenReturn(saved);
        var result = stokService.olustur(dto, 1L);
        assertEquals("STK999", result.getStokKodu());
        verify(barkodUretService).ean13Uret(1L);
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
    void olustur_blankStokKoduIcinKodUretirVeBarkodNormalizeEder() {
        StokDTO dto = StokDTO.builder().stokKodu("   ").ad("Kodsuz Urun").birim("Adet")
                .fiyat(BigDecimal.valueOf(50)).barkod("  ").build();
        when(barkodUretService.ean13Uret(1L)).thenReturn("8690000000005");
        when(stokRepository.save(any(Stok.class))).thenAnswer(inv -> inv.getArgument(0));
        var result = stokService.olustur(dto, 1L);
        assertNotNull(result.getStokKodu());
        assertTrue(result.getStokKodu().startsWith("STK-"));
        // Bos barkod artik otomatik EAN-13 ile doldurulur.
        assertEquals("8690000000005", result.getBarkod());
    }

    @Test
    void olustur_stokKoduTrimlenir() {
        StokDTO dto = StokDTO.builder().stokKodu("  ABC-1  ").ad("Trimli").birim("Adet")
                .fiyat(BigDecimal.valueOf(50)).build();
        when(stokRepository.save(any(Stok.class))).thenAnswer(inv -> inv.getArgument(0));
        var result = stokService.olustur(dto, 1L);
        assertEquals("ABC-1", result.getStokKodu());
    }

    @Test
    void guncelle_blankStokKoduMevcutKoduKorur() {
        Stok existing = createStok(1L);
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(stokRepository.save(any(Stok.class))).thenAnswer(inv -> inv.getArgument(0));
        StokDTO dto = StokDTO.builder().stokKodu(null).ad("Guncel").birim("Adet")
                .fiyat(BigDecimal.valueOf(150)).build();
        var result = stokService.guncelle(1L, dto);
        assertEquals("STK001", result.getStokKodu());
    }

    @Test
    void barkodIleBul_mukerrerBarkodIlkKaydiDoner() {
        Stok ilk = createStok(1L);
        Stok ikinci = createStok(2L);
        when(stokRepository.findBySirketIdAndBarkod(1L, "BAR001")).thenReturn(List.of(ilk, ikinci));
        var result = stokService.barkodIleBul("BAR001", 1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
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
    void hareketler_izBilgileriniDoldurur() {
        Stok stok = createStok(1L);
        StokHareket h = StokHareket.builder().id(1L).stok(stok).tur("CIKIS").miktar(BigDecimal.valueOf(2))
                .hareketTarihi(LocalDate.now()).depoId(7L).seriId(9L)
                .kaynakTip("FATURA").kaynakId(55L).build();
        when(stokRepository.findById(1L)).thenReturn(Optional.of(stok));
        when(stokHareketRepository.findByStokIdOrderByHareketTarihiDesc(1L)).thenReturn(List.of(h));
        when(depoRepository.findById(7L)).thenReturn(Optional.of(
                com.raspel.erp.entity.sube.Depo.builder().id(7L).ad("Merkez Depo").build()));
        when(stokSeriRepository.findById(9L)).thenReturn(Optional.of(
                com.raspel.erp.entity.envanter.StokSeri.builder().id(9L).seriNo("SN-9").build()));

        var result = stokService.hareketler(1L);

        assertEquals("Merkez Depo", result.get(0).getDepoAd());
        assertEquals("SN-9", result.get(0).getSeriNo());
        assertEquals("FATURA", result.get(0).getKaynakTip());
        assertEquals(55L, result.get(0).getKaynakId());
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
        when(stokHareketRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(h));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
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
        when(stokRepository.toplamMiktarBySirketId(1L)).thenReturn(BigDecimal.valueOf(100));
        BigDecimal total = stokService.toplamStokMiktari(1L);
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
        when(stokHareketRepository.sonCikisToplamlari(any(), any()))
                .thenReturn(List.of(java.util.Map.of("stokId", 1L, "toplam", BigDecimal.valueOf(90))));

        var result = stokService.talepTahmini(1L);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getStokId());
        assertTrue(result.get(0).getTahminiTukenmeGunu() > 0);
        assertNotNull(result.get(0).getProaktifOneri());
    }

    @Test
    void enCokSatanlar_nullSirketBosDoner() {
        assertTrue(stokService.enCokSatanlar(null, 12).isEmpty());
    }

    @Test
    void enCokSatanlar_satislaraGoreStokDoner() {
        when(stokHareketRepository.enCokSatanlarBySirket(1L)).thenReturn(List.of(
                java.util.Map.of("stokAd", "Urun 1", "stokKodu", "STK001", "satisMiktari", BigDecimal.valueOf(10))
        ));
        Stok stok = createStok(1L);
        stok.setSirketId(1L);
        when(stokRepository.findBySirketIdAndStokKoduIn(eq(1L), org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(stok));

        var result = stokService.enCokSatanlar(1L, 12);

        assertEquals(1, result.size());
        assertEquals("Urun 1", result.get(0).getAd());
    }

    @Test
    void fiyatlariGetir_listeler() {
        Stok stok = createStok(1L);
        stok.setSirketId(1L);
        when(stokRepository.findById(1L)).thenReturn(Optional.of(stok));
        when(stokFiyatRepository.findByStokIdOrderByFiyatAsc(1L)).thenReturn(List.of(
                com.raspel.erp.entity.envanter.StokFiyat.builder().id(2L).stokId(1L).ad("Toptan").fiyat(BigDecimal.valueOf(80)).sirketId(1L).build(),
                com.raspel.erp.entity.envanter.StokFiyat.builder().id(1L).stokId(1L).ad("Perakende").fiyat(BigDecimal.valueOf(100)).sirketId(1L).build()
        ));

        var result = stokService.fiyatlariGetir(1L);

        assertEquals(2, result.size());
        assertEquals("Toptan", result.get(0).getAd());
    }

    @Test
    void fiyatEkle_kaydeder() {
        Stok stok = createStok(1L);
        stok.setSirketId(1L);
        when(stokRepository.findById(1L)).thenReturn(Optional.of(stok));
        when(stokFiyatRepository.save(any())).thenAnswer(inv -> {
            com.raspel.erp.entity.envanter.StokFiyat f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });

        var result = stokService.fiyatEkle(1L,
                com.raspel.erp.dto.envanter.StokFiyatDTO.builder().ad("Kurumsal").fiyat(BigDecimal.valueOf(90)).build(), 1L);

        assertEquals("Kurumsal", result.getAd());
        assertEquals(0, result.getFiyat().compareTo(BigDecimal.valueOf(90)));
    }

    // hareketler() eskiden bos map gecirdi; hareketToDTO her satir icin ayri
    // findById cagirip depo/seri adi cozdugu icin hareket sayisi kadar ek sorgu
    // uretiliyordu (N+1). Artik depo/seri adlari findAllById ile tek seferde alinir.
    @Test
    void hareketler_depoVeSeriAdlariniTopluCozer() {
        Stok stok = createStok(1L);
        stok.setSirketId(1L);
        when(stokRepository.findById(1L)).thenReturn(Optional.of(stok));

        StokHareket h1 = hareket(1L, stok, 10L, 100L);
        StokHareket h2 = hareket(2L, stok, 10L, 101L);
        when(stokHareketRepository.findByStokIdOrderByHareketTarihiDesc(1L)).thenReturn(List.of(h1, h2));
        when(depoRepository.findAllById(Set.of(10L))).thenReturn(List.of(
                com.raspel.erp.entity.sube.Depo.builder().id(10L).ad("Merkez Depo").build()));
        when(stokSeriRepository.findAllById(Set.of(100L, 101L))).thenReturn(List.of(
                com.raspel.erp.entity.envanter.StokSeri.builder().id(100L).seriNo("SN-1").build(),
                com.raspel.erp.entity.envanter.StokSeri.builder().id(101L).seriNo("SN-2").build()));

        List<StokHareketDTO> sonuc = stokService.hareketler(1L);

        assertEquals(2, sonuc.size());
        assertEquals("Merkez Depo", sonuc.get(0).getDepoAd());
        assertEquals("Merkez Depo", sonuc.get(1).getDepoAd());
        assertEquals("SN-1", sonuc.get(0).getSeriNo());
        assertEquals("SN-2", sonuc.get(1).getSeriNo());

        verify(depoRepository, times(1)).findAllById(any());
        verify(stokSeriRepository, times(1)).findAllById(any());
        verify(depoRepository, never()).findById(any());
        verify(stokSeriRepository, never()).findById(any());
    }

    @Test
    void hareketler_depoVeSeriIdleriYoksaEkSorguYapmaz() {
        Stok stok = createStok(1L);
        stok.setSirketId(1L);
        when(stokRepository.findById(1L)).thenReturn(Optional.of(stok));
        StokHareket h = hareket(1L, stok, null, null);
        when(stokHareketRepository.findByStokIdOrderByHareketTarihiDesc(1L)).thenReturn(List.of(h));

        List<StokHareketDTO> sonuc = stokService.hareketler(1L);

        assertEquals(1, sonuc.size());
        assertNull(sonuc.get(0).getDepoAd());
        assertNull(sonuc.get(0).getSeriNo());
        verify(depoRepository, never()).findAllById(any());
        verify(stokSeriRepository, never()).findAllById(any());
    }

    private StokHareket hareket(Long id, Stok stok, Long depoId, Long seriId) {
        StokHareket h = StokHareket.builder()
                .id(id).stok(stok).tur("GIRIS").miktar(BigDecimal.ONE)
                .hareketTarihi(LocalDate.of(2026, 1, id.intValue()))
                .build();
        h.setDepoId(depoId);
        h.setSeriId(seriId);
        return h;
    }

    // ---------- B4c: toplu fiyat guncelleme (double -> BigDecimal, oran dogrulama) ----------

    private Stok fiyatliStok(Long id, String fiyat, String satis) {
        Stok s = new Stok();
        s.setId(id);
        s.setAd("Stok " + id);
        s.setFiyat(new BigDecimal(fiyat));
        s.setSatisFiyati(new BigDecimal(satis));
        return s;
    }

    /**
     * B4c: eski kullanim `double carpan` idi ve her satirda
     * `BigDecimal.valueOf(double)` cagriliyordu. double ikili temsil
     * hatasi nedeniyle 100,00 x %15 = 114,99999... yerine kesin
     * 115,00 vermeli.
     */
    @Test
    void topluFiyatGuncelle_artistaKesinYuzdeUygular() {
        Stok s = fiyatliStok(1L, "100.00", "200.00");
        when(stokRepository.findBySirketIdOrderByAd(eq(1L), any())).thenReturn(new PageImpl<>(List.of(s)));

        int n = stokService.topluFiyatGuncelle(
                com.raspel.erp.dto.envanter.TopluFiyatDTO.builder().yon("ARTIR").oran(15.0).build(), 1L);

        assertEquals(1, n);
        assertEquals(0, new BigDecimal("115.00").compareTo(s.getFiyat()));
        assertEquals(0, new BigDecimal("230.00").compareTo(s.getSatisFiyati()));
    }

    @Test
    void topluFiyatGuncelle_azaltMamulYuzdeUygular() {
        Stok s = fiyatliStok(1L, "100.00", "200.00");
        when(stokRepository.findBySirketIdOrderByAd(eq(1L), any())).thenReturn(new PageImpl<>(List.of(s)));

        stokService.topluFiyatGuncelle(
                com.raspel.erp.dto.envanter.TopluFiyatDTO.builder().yon("AZALT").oran(10.0).build(), 1L);

        assertEquals(0, new BigDecimal("90.00").compareTo(s.getFiyat()));
        assertEquals(0, new BigDecimal("180.00").compareTo(s.getSatisFiyati()));
    }

/** B4c: negatif oran reddedilir (aksi halde fiyat isareti bozulur). */
    @Test
    void topluFiyatGuncelle_negatifOranReddedilir() {
        assertThrows(BusinessException.class, () -> stokService.topluFiyatGuncelle(
                com.raspel.erp.dto.envanter.TopluFiyatDTO.builder().yon("ARTIR").oran(-50.0).build(), 1L));
        // Fail-fast: gecersiz oranda tablo HIC sorgulanmaz.
        verify(stokRepository, never()).findBySirketIdOrderByAd(any(), any());
        verify(stokRepository, never()).saveAll(any());
    }

    /** B4c: AZALT yonunde %100'den buyuk oran fiyati negatife cevirirdi. */
    @Test
    void topluFiyatGuncelle_yuzdenBuyukAzaltmaReddedilir() {
        assertThrows(BusinessException.class, () -> stokService.topluFiyatGuncelle(
                com.raspel.erp.dto.envanter.TopluFiyatDTO.builder().yon("AZALT").oran(150.0).build(), 1L));
        verify(stokRepository, never()).saveAll(any());
    }

    /** B4c: %100 azaltma fiyati sifira indirir, hata vermez. */
    @Test
    void topluFiyatGuncelle_yuzAzaltmaSifirYapar() {
        Stok s = fiyatliStok(1L, "100.00", "80.00");
        when(stokRepository.findBySirketIdOrderByAd(eq(1L), any())).thenReturn(new PageImpl<>(List.of(s)));

        stokService.topluFiyatGuncelle(
                com.raspel.erp.dto.envanter.TopluFiyatDTO.builder().yon("AZALT").oran(100.0).build(), 1L);

        assertEquals(0, BigDecimal.ZERO.compareTo(s.getFiyat()));
        assertEquals(0, BigDecimal.ZERO.compareTo(s.getSatisFiyati()));
    }

    /** B4c: oran null ise carpan 1 -> fiyat degismez. */
    @Test
    void topluFiyatGuncelle_oranNullFiyatiDegistirmez() {
        Stok s = fiyatliStok(1L, "100.00", "200.00");
        when(stokRepository.findBySirketIdOrderByAd(eq(1L), any())).thenReturn(new PageImpl<>(List.of(s)));

        stokService.topluFiyatGuncelle(
                com.raspel.erp.dto.envanter.TopluFiyatDTO.builder().yon("ARTIR").build(), 1L);

        assertEquals(0, new BigDecimal("100.00").compareTo(s.getFiyat()));
        assertEquals(0, new BigDecimal("200.00").compareTo(s.getSatisFiyati()));
    }
}