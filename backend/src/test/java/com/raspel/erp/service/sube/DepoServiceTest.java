package com.raspel.erp.service.sube;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sube.DepoDTO;
import com.raspel.erp.dto.sube.DepoStokDTO;
import com.raspel.erp.entity.sube.Depo;
import com.raspel.erp.entity.sube.DepoStok;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.sube.DepoRepository;
import com.raspel.erp.repository.sube.DepoStokRepository;
import com.raspel.erp.repository.sube.SubeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepoServiceTest {

    @Mock private DepoRepository depoRepository;
    @Mock private DepoStokRepository depoStokRepository;
    @Mock private SubeRepository subeRepository;
    @Mock private StokRepository stokRepository;
    @Mock private TenantChecker tenantChecker;
    @Mock private com.raspel.erp.service.envanter.StokService stokService;
    @Mock private com.raspel.erp.repository.sube.DepoTransferRepository depoTransferRepository;
    @Mock private com.raspel.erp.repository.envanter.StokHareketRepository stokHareketRepository;
    @Mock private com.raspel.erp.repository.sistem.KullaniciRepository kullaniciRepository;
    @Mock private DepoYetki depoYetki;
    @InjectMocks private DepoService depoService;

    private Depo ornekDepo(Long id) {
        return Depo.builder()
                .id(id).ad("Ana Depo").adres("İstanbul")
                .subeId(1L).sirketId(1L).aktif(true)
                .olusturmaTarihi(LocalDateTime.now())
                .build();
    }

    @Test
    void tumunuGetir_returnsPage() {
        when(subeRepository.findBySirketIdOrderByAdAsc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(depoRepository.filtreli(eq(1L), isNull(), isNull(), isNull(), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornekDepo(1L))));
        var sonuc = depoService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, sonuc.getContent().size());
        assertEquals("Ana Depo", sonuc.getContent().get(0).getAd());
    }

    @Test
    void tumunuGetir_filtreleriGecer() {
        when(subeRepository.findBySirketIdOrderByAdAsc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(depoRepository.filtreli(eq(1L), eq("%merkez%"), eq(2L), eq(true), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        depoService.tumunuGetir(1L, " Merkez ", 2L, true, true, Pageable.unpaged());

        verify(depoRepository).filtreli(eq(1L), eq("%merkez%"), eq(2L), eq(true), eq(true), any(Pageable.class));
    }

    @Test
    void tumunuGetir_istatistikleriMapEder() {
        Depo depo = ornekDepo(1L);
        depo.setKapasiteKalem(10);
        when(subeRepository.findBySirketIdOrderByAdAsc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(depoRepository.filtreli(eq(1L), isNull(), isNull(), isNull(), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(depo)));
        when(depoStokRepository.depoIstatistikleri(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[] { 1L, 4L, 1L, 2L, new BigDecimal("1200.00") }));

        var dto = depoService.tumunuGetir(1L, Pageable.unpaged()).getContent().get(0);

        assertEquals(4L, dto.getKalemSayisi());
        assertEquals(1L, dto.getSifirStokSayisi());
        assertEquals(2L, dto.getKritikStokSayisi());
        assertEquals(0, dto.getStokDegeri().compareTo(new BigDecimal("1200.00")));
        assertEquals(40, dto.getDolulukYuzde());
        assertTrue(dto.getKritikMi());
    }

    @Test
    void tumunuGetir_kapasiteYoksaDolulukNull() {
        when(subeRepository.findBySirketIdOrderByAdAsc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(depoRepository.filtreli(eq(1L), isNull(), isNull(), isNull(), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornekDepo(1L))));
        when(depoStokRepository.depoIstatistikleri(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[] { 1L, 4L, 0L, 0L, BigDecimal.ZERO }));

        var dto = depoService.tumunuGetir(1L, Pageable.unpaged()).getContent().get(0);

        assertNull(dto.getDolulukYuzde());
        assertFalse(dto.getKritikMi());
    }

    @Test
    void getir_returnsById() {
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(subeRepository.findById(1L)).thenReturn(Optional.empty());
        var sonuc = depoService.getir(1L);
        assertEquals("Ana Depo", sonuc.getAd());
    }

    @Test
    void getir_notFound_throws() {
        when(depoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> depoService.getir(99L));
    }

    @Test
    void olustur_creates() {
        DepoDTO dto = DepoDTO.builder().ad("Yeni Depo").subeId(1L).sirketId(1L).build();
        when(depoRepository.save(any(Depo.class))).thenAnswer(inv -> {
            Depo d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });
        var sonuc = depoService.olustur(dto);
        assertEquals("Yeni Depo", sonuc.getAd());
    }

    @Test
    void stokEkle_globalHareketOlusturur() {
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(stokRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of());
        when(depoStokRepository.findByDepoId(1L)).thenReturn(List.of());

        depoService.stokEkle(1L, 2L, new BigDecimal("5"));

        verify(stokService).hareketEkle(argThat(d -> "GIRIS".equals(d.getTur())
                && Long.valueOf(1L).equals(d.getDepoId())
                && d.getMiktar().compareTo(new BigDecimal("5")) == 0));
    }

    @Test
    void stokCikar_yetersizStok_throws() {
        DepoStok mevcut = DepoStok.builder().depoId(1L).stokId(2L).miktar(new BigDecimal("3")).build();
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(depoStokRepository.findByDepoIdAndStokIdForUpdate(1L, 2L)).thenReturn(Optional.of(mevcut));

        assertThrows(BusinessException.class, () -> depoService.stokCikar(1L, 2L, new BigDecimal("10")));
    }

    @Test
    void stokCikar_azaltirMiktari() {
        DepoStok mevcut = DepoStok.builder().depoId(1L).stokId(2L).miktar(new BigDecimal("20")).build();
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(depoStokRepository.findByDepoIdAndStokIdForUpdate(1L, 2L)).thenReturn(Optional.of(mevcut));
        when(stokRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of());
        when(depoStokRepository.findByDepoId(1L)).thenReturn(List.of(mevcut));

        depoService.stokCikar(1L, 2L, new BigDecimal("8"));

        verify(stokService).hareketEkle(argThat(d -> "CIKIS".equals(d.getTur())
                && Long.valueOf(1L).equals(d.getDepoId())
                && d.getMiktar().compareTo(new BigDecimal("8")) == 0));
    }

    @Test
    void stokTransfer_ikisiniDeYapar() {
        DepoStok kaynak = DepoStok.builder().depoId(1L).stokId(2L).miktar(new BigDecimal("50")).build();
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(depoRepository.findById(3L)).thenReturn(Optional.of(ornekDepo(3L)));
        when(depoStokRepository.findByDepoIdAndStokIdForUpdate(1L, 2L)).thenReturn(Optional.of(kaynak));
        when(stokRepository.findBySirketIdOrderByAd(anyLong())).thenReturn(List.of());
        when(depoStokRepository.findByDepoId(anyLong())).thenReturn(List.of());

        depoService.stokTransfer(1L, 3L, 2L, new BigDecimal("10"));

        verify(stokService).hareketEkle(argThat(d -> "CIKIS".equals(d.getTur()) && Long.valueOf(1L).equals(d.getDepoId())));
        verify(stokService).hareketEkle(argThat(d -> "GIRIS".equals(d.getTur()) && Long.valueOf(3L).equals(d.getDepoId())));
    }

    @Test
    void stokDagilimi_depoKiriliminiDoner() {
        when(depoRepository.findBySirketIdOrderByAdAsc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornekDepo(1L))));
        DepoStok ds = DepoStok.builder().id(5L).depoId(1L).stokId(2L).miktar(new BigDecimal("7")).build();
        when(depoStokRepository.findByDepoIdIn(any())).thenReturn(List.of(ds));

        var sonuc = depoService.stokDagilimi(1L);

        assertEquals(1, sonuc.size());
        assertEquals("Ana Depo", sonuc.get(0).getDepoAdi());
        assertEquals(0, sonuc.get(0).getMiktar().compareTo(new BigDecimal("7")));
    }

    @Test
    void stokDagilimi_depoYoksaBosDoner() {
        when(depoRepository.findBySirketIdOrderByAdAsc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        assertTrue(depoService.stokDagilimi(1L).isEmpty());
    }

    @Test
    void detayOzet_kpiVeSonHareketleriDoner() {
        Depo depo = ornekDepo(1L);
        depo.setKapasiteKalem(20);
        when(depoRepository.findById(1L)).thenReturn(Optional.of(depo));
        when(depoStokRepository.depoIstatistikleri(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[] { 1L, 5L, 2L, 1L, new BigDecimal("500.00") }));
        com.raspel.erp.entity.envanter.Stok stok = com.raspel.erp.entity.envanter.Stok.builder()
                .id(2L).ad("Vida").stokKodu("V-1").build();
        com.raspel.erp.entity.envanter.StokHareket h = com.raspel.erp.entity.envanter.StokHareket.builder()
                .id(5L).stok(stok).tur("GIRIS").miktar(new BigDecimal("3"))
                .hareketTarihi(java.time.LocalDate.now()).kullaniciId(9L)
                .olusturmaTarihi(LocalDateTime.now()).build();
        when(stokHareketRepository.findTop5ByDepoIdOrderByOlusturmaTarihiDesc(1L)).thenReturn(List.of(h));
        when(kullaniciRepository.findAllById(java.util.Set.of(9L)))
                .thenReturn(List.of(com.raspel.erp.entity.sistem.Kullanici.builder().id(9L).displayName("Ali").build()));

        var ozet = depoService.detayOzet(1L);

        assertEquals(5L, ozet.getKalemSayisi());
        assertEquals(2L, ozet.getSifirStokSayisi());
        assertEquals(1L, ozet.getKritikStokSayisi());
        assertEquals(25, ozet.getDolulukYuzde());
        assertEquals(1, ozet.getSonHareketler().size());
        assertEquals("Ali", ozet.getSonHareketler().get(0).getKullaniciAd());
        assertEquals("Vida", ozet.getSonHareketler().get(0).getStokAd());
    }

    @Test
    void stokEkle_negatifMiktar_throws() {
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        assertThrows(BusinessException.class,
                () -> depoService.stokEkle(1L, 2L, new BigDecimal("0")));
    }

    @Test
    void stokEkle_yetkisizKullanici_throws() {
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(depoYetki.islemYapabilir(any(Depo.class), eq(9L))).thenReturn(false);
        assertThrows(BusinessException.class,
                () -> depoService.stokEkle(1L, 2L, new BigDecimal("1"), 9L));
        verifyNoInteractions(stokService);
    }

    @Test
    void stokSayfali_degerVeKritikHesaplar() {
        Depo depo = ornekDepo(1L);
        when(depoRepository.findById(1L)).thenReturn(Optional.of(depo));
        DepoStok ds = DepoStok.builder().id(5L).depoId(1L).stokId(2L).miktar(new BigDecimal("3")).build();
        when(depoStokRepository.sayfali(eq(1L), isNull(), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ds)));
        com.raspel.erp.entity.envanter.Stok stok = com.raspel.erp.entity.envanter.Stok.builder()
                .id(2L).ad("Vida").stokKodu("V-1").birim("Adet")
                .minMiktar(new BigDecimal("5")).ortalamaMaliyet(new BigDecimal("10")).build();
        when(stokRepository.findAllById(List.of(2L))).thenReturn(List.of(stok));

        var dto = depoService.stokSayfali(1L, null, null, null, Pageable.unpaged()).getContent().get(0);

        assertEquals(0, dto.getDeger().compareTo(new BigDecimal("30")));
        assertTrue(dto.getKritikMi());
        assertEquals("Vida", dto.getStokAd());
    }

    @Test
    void hareketler_kullaniciAdiniCozer() {
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        com.raspel.erp.entity.envanter.Stok stok = com.raspel.erp.entity.envanter.Stok.builder()
                .id(2L).ad("Vida").stokKodu("V-1").build();
        com.raspel.erp.entity.envanter.StokHareket h = com.raspel.erp.entity.envanter.StokHareket.builder()
                .id(5L).stok(stok).tur("GIRIS").miktar(new BigDecimal("3"))
                .hareketTarihi(java.time.LocalDate.now()).kullaniciId(9L)
                .olusturmaTarihi(LocalDateTime.now()).build();
        when(stokHareketRepository.findByDepoIdOrderByOlusturmaTarihiDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(h)));
        when(kullaniciRepository.findAllById(java.util.Set.of(9L)))
                .thenReturn(List.of(com.raspel.erp.entity.sistem.Kullanici.builder().id(9L).displayName("Ali").build()));

        var dto = depoService.hareketler(1L, null, Pageable.unpaged()).getContent().get(0);

        assertEquals("Ali", dto.getKullaniciAd());
        assertEquals("Vida", dto.getStokAd());
    }

    @Test
    void stokTransfer_ayniDepo_throws() {
        assertThrows(BusinessException.class,
                () -> depoService.stokTransfer(1L, 1L, 2L, new BigDecimal("1")));
    }

    @Test
    void sil_deletes() {
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        depoService.sil(1L);
        verify(depoRepository).deleteById(1L);
    }

    @Test
    void ozet_sayimlariDondurur() {
        when(depoRepository.countBySirketId(1L)).thenReturn(5L);
        when(depoRepository.countBySirketIdAndAktifTrue(1L)).thenReturn(4L);
        when(depoTransferRepository.countBySirketIdAndDurum(1L, "BEKLIYOR")).thenReturn(2L);
        when(depoStokRepository.ozet(1L)).thenReturn(new Object[] { 10L, 3L });

        var ozet = depoService.ozet(1L);

        assertEquals(5, ozet.getToplamDepo());
        assertEquals(4, ozet.getAktifDepo());
        assertEquals(1, ozet.getPasifDepo());
        assertEquals(10, ozet.getStokKalemiSayisi());
        assertEquals(3, ozet.getSifirStokluKalem());
        assertEquals(2, ozet.getBekleyenTalep());
    }

    @Test
    void ozet_sirketYoksaSifirDoner() {
        var ozet = depoService.ozet(null);
        assertEquals(0, ozet.getToplamDepo());
        verifyNoInteractions(depoRepository);
    }
}
