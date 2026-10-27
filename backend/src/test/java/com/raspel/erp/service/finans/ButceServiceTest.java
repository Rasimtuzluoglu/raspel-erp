package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.ButceDTO;
import com.raspel.erp.entity.finans.Butce;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.ButceRepository;
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
class ButceServiceTest {

    @Mock private ButceRepository butceRepository;
    @Mock private com.raspel.erp.repository.finans.ButceRevizyonRepository butceRevizyonRepository;
    @Mock private com.raspel.erp.repository.finans.MasrafRepository masrafRepository;
    @Mock private com.raspel.erp.repository.finans.HareketRepository hareketRepository;
    @Mock private com.raspel.erp.service.sistem.AuditLogService auditLogService;
    @Mock private TenantChecker tenantChecker;
    @InjectMocks private ButceService butceService;

    private Butce ornekButce(Long id) {
        return Butce.builder()
                .id(id).ad("2026 Bütçe").yil(2026).ay(1)
                .tutar(new BigDecimal("50000")).tur("GIDER")
                .kategori("Pazarlama").sirketId(1L).olusturmaTarihi(LocalDateTime.now())
                .build();
    }

    @Test
    void tumunuGetir_returnsPage() {
        when(butceRepository.findBySirketIdOrderByYilDescAyDesc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornekButce(1L))));
        var sonuc = butceService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, sonuc.getContent().size());
        assertEquals("2026 Bütçe", sonuc.getContent().get(0).getAd());
    }

    @Test
    void getir_returnsById() {
        when(butceRepository.findById(1L)).thenReturn(Optional.of(ornekButce(1L)));
        var sonuc = butceService.getir(1L);
        assertEquals(2026, sonuc.getYil());
    }

    @Test
    void getir_notFound_throws() {
        when(butceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> butceService.getir(99L));
    }

    @Test
    void olustur_creates() {
        ButceDTO dto = ButceDTO.builder().ad("Yeni Bütçe").yil(2026).ay(3)
                .tutar(new BigDecimal("10000")).tur("GELIR").build();
        when(butceRepository.save(any(Butce.class))).thenAnswer(inv -> {
            Butce b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });
        var sonuc = butceService.olustur(dto, 1L);
        assertEquals("Yeni Bütçe", sonuc.getAd());
        assertEquals(1L, sonuc.getSirketId());
    }

    @Test
    void guncelle_updatesOnlyNonNull() {
        Butce butce = ornekButce(1L);
        when(butceRepository.findById(1L)).thenReturn(Optional.of(butce));
        when(butceRepository.save(any(Butce.class))).thenAnswer(inv -> inv.getArgument(0));

        ButceDTO dto = ButceDTO.builder().ad("Güncel Bütçe").build();
        var sonuc = butceService.guncelle(1L, dto);
        assertEquals("Güncel Bütçe", sonuc.getAd());
        assertEquals(2026, sonuc.getYil());
        // Onaylı bütçe düzenlenince revizyon artar ve yeniden onaya düşer.
        assertEquals(2, sonuc.getRevizyon());
        assertEquals("TASLAK", sonuc.getDurum());
    }

    @Test
    void guncelle_reddedilmisButceyiYenidenTaslakYapar() {
        Butce butce = ornekButce(1L);
        butce.setDurum("REDDEDILDI");
        butce.setRedGerekcesi("Tutar fazla");
        when(butceRepository.findById(1L)).thenReturn(Optional.of(butce));
        when(butceRepository.save(any(Butce.class))).thenAnswer(inv -> inv.getArgument(0));

        var sonuc = butceService.guncelle(1L, ButceDTO.builder().tutar(new BigDecimal("45000")).build());

        assertEquals("TASLAK", sonuc.getDurum());
        assertNull(sonuc.getRedGerekcesi());
        assertEquals(2, sonuc.getRevizyon());
    }

    @Test
    void sil_deletes() {
        when(butceRepository.findById(1L)).thenReturn(Optional.of(ornekButce(1L)));
        butceService.sil(1L);
        verify(butceRepository).deleteById(1L);
    }

    @Test
    void sil_notFound_throws() {
        when(butceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> butceService.sil(99L));
    }

    @Test
    void filtreli_sunucuFiltreleriniIletir() {
        when(butceRepository.filtreliGetir(eq(1L), eq(2026), eq(3), eq("GIDER"),
                isNull(), eq("Pazarlama"), eq(com.raspel.erp.util.AramaTemizleyici.like("kampanya")), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornekButce(1L))));

        var sonuc = butceService.filtreli(1L, 2026, 3, "gider", " Pazarlama ", null,
                "kampanya", Pageable.unpaged());

        assertEquals(1, sonuc.getContent().size());
    }

    @Test
    void ozet_giderTuruIcinMasrafGercekleseniniKullanir() {
        when(butceRepository.toplamTutar(1L, 2026, "GELIR")).thenReturn(new BigDecimal("1000"));
        when(butceRepository.toplamTutar(1L, 2026, "GIDER")).thenReturn(new BigDecimal("5000"));
        when(hareketRepository.sumTutarBySirketIdAndTurAndTarihBetween(
                eq(1L), eq(com.raspel.erp.entity.finans.Hareket.HareketTuru.TAHSILAT), any(), any()))
                .thenReturn(new BigDecimal("2000"));
        when(masrafRepository.sumTutarBySirketIdAndTarihBetween(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("4000"));
        when(butceRepository.sayi(1L, 2026, "GIDER")).thenReturn(3L);
        when(butceRepository.kategoriler(1L, 2026, "GIDER")).thenReturn(List.of("A", "B"));

        var ozet = butceService.ozet(1L, 2026, "GIDER");

        assertEquals(0, ozet.getButceToplam().compareTo(new BigDecimal("5000.00")));
        assertEquals(0, ozet.getGerceklesenToplam().compareTo(new BigDecimal("4000.00")));
        assertEquals(0, ozet.getSapma().compareTo(new BigDecimal("-1000.00")));
        assertEquals(0, ozet.getKullanimYuzde().compareTo(new BigDecimal("80.0")));
        assertEquals(3L, ozet.getKayitSayisi());
        assertEquals(2L, ozet.getKategoriSayisi());
        assertEquals(0, ozet.getGelirGerceklesen().compareTo(new BigDecimal("2000.00")));
    }

    @Test
    void ozet_turYoksaIkisiniToplar() {
        when(butceRepository.toplamTutar(1L, 2026, "GELIR")).thenReturn(new BigDecimal("1000"));
        when(butceRepository.toplamTutar(1L, 2026, "GIDER")).thenReturn(new BigDecimal("500"));
        when(hareketRepository.sumTutarBySirketIdAndTurAndTarihBetween(
                eq(1L), any(), any(), any())).thenReturn(new BigDecimal("800"));
        when(masrafRepository.sumTutarBySirketIdAndTarihBetween(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("200"));
        when(butceRepository.sayi(1L, 2026, null)).thenReturn(2L);
        when(butceRepository.kategoriler(1L, 2026, null)).thenReturn(List.of("A"));

        var ozet = butceService.ozet(1L, 2026, null);

        assertEquals(0, ozet.getButceToplam().compareTo(new BigDecimal("1500.00")));
        assertEquals(0, ozet.getGerceklesenToplam().compareTo(new BigDecimal("1000.00")));
        assertEquals(0, ozet.getKullanimYuzde().compareTo(new BigDecimal("66.7")));
    }

    @Test
    void ozet_butceSifirsaKullanimNullDoner() {
        when(butceRepository.toplamTutar(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(hareketRepository.sumTutarBySirketIdAndTurAndTarihBetween(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(masrafRepository.sumTutarBySirketIdAndTarihBetween(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(butceRepository.sayi(any(), any(), any())).thenReturn(0L);
        when(butceRepository.kategoriler(any(), any(), any())).thenReturn(List.of());

        var ozet = butceService.ozet(1L, 2026, "GELIR");

        assertNull(ozet.getKullanimYuzde());
        assertEquals(0, ozet.getSapma().compareTo(BigDecimal.ZERO));
    }

    @Test
    void gerceklesen_giderIcinKategoriBazliMasrafiKarsilastirir() {
        Butce p = Butce.builder().id(1L).sirketId(1L).yil(2026).ay(3).tur("GIDER")
                .kategori("Pazarlama").tutar(new BigDecimal("1000")).build();
        Butce k = Butce.builder().id(2L).sirketId(1L).yil(2026).ay(3).tur("GIDER")
                .kategori("Kira").tutar(new BigDecimal("500")).build();
        Butce gelir = Butce.builder().id(3L).sirketId(1L).yil(2026).ay(3).tur("GELIR")
                .kategori("Satış").tutar(new BigDecimal("9000")).build();
        when(butceRepository.findBySirketIdOrderByYilDescAyDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p, k, gelir)));
        when(masrafRepository.findBySirketIdAndTarihBetween(eq(1L), any(), any()))
                .thenReturn(List.of(
                        com.raspel.erp.entity.finans.Masraf.builder().kategori("Pazarlama")
                                .tutar(new BigDecimal("700")).build(),
                        com.raspel.erp.entity.finans.Masraf.builder().kategori("Kira")
                                .tutar(new BigDecimal("200")).build(),
                        com.raspel.erp.entity.finans.Masraf.builder().kategori("Diğer")
                                .tutar(new BigDecimal("100")).build()));

        var rapor = butceService.gerceklesen(1L, 2026, 3, "GIDER");

        assertEquals("GIDER", rapor.getTur());
        assertEquals(0, rapor.getButceToplam().compareTo(new BigDecimal("1500.00")));
        assertEquals(0, rapor.getGerceklesenToplam().compareTo(new BigDecimal("1000.00")));
        assertEquals(0, rapor.getSapma().compareTo(new BigDecimal("-500.00")));
        assertEquals(0, rapor.getKullanimYuzde().compareTo(new BigDecimal("66.7")));
        // Diğer kategorisi bütçede yok ama masrafta var; raporda görünmeli.
        assertEquals(3, rapor.getSatirlar().size());
        assertEquals(0, rapor.getSatirlar().stream()
                .filter(s -> "Diğer".equals(s.getKategori()))
                .findFirst().orElseThrow().getSapma().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void gerceklesen_gelirIcinTahsilatToplaminiKullanir() {
        Butce satis = Butce.builder().id(1L).sirketId(1L).yil(2026).ay(3).tur("GELIR")
                .kategori("Satış").tutar(new BigDecimal("2000")).build();
        Butce kira = Butce.builder().id(2L).sirketId(1L).yil(2026).ay(3).tur("GELIR")
                .kategori("Kira").tutar(new BigDecimal("1000")).build();
        when(butceRepository.findBySirketIdOrderByYilDescAyDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(satis, kira)));
        when(hareketRepository.sumTutarBySirketIdAndTurAndTarihBetween(
                eq(1L), eq(com.raspel.erp.entity.finans.Hareket.HareketTuru.TAHSILAT), any(), any()))
                .thenReturn(new BigDecimal("2500"));

        var rapor = butceService.gerceklesen(1L, 2026, 3, "GELIR");

        assertEquals(0, rapor.getButceToplam().compareTo(new BigDecimal("3000.00")));
        assertEquals(0, rapor.getGerceklesenToplam().compareTo(new BigDecimal("2500.00")));
        assertEquals(0, rapor.getKullanimYuzde().compareTo(new BigDecimal("83.3")));
        assertEquals("Tahsilat", rapor.getSatirlar().get(0).getKategori());
        assertEquals(3, rapor.getSatirlar().size());
        // Kategori satırlarında plan var, gerçekleşen yok.
        assertNull(rapor.getSatirlar().get(1).getGerceklesen());
    }

    @Test
    void gerceklesen_butceYoksaKullanimNullDoner() {
        when(butceRepository.findBySirketIdOrderByYilDescAyDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(masrafRepository.findBySirketIdAndTarihBetween(eq(1L), any(), any()))
                .thenReturn(List.of());

        var rapor = butceService.gerceklesen(1L, 2026, null, "GIDER");

        assertEquals(0, rapor.getButceToplam().compareTo(BigDecimal.ZERO));
        assertNull(rapor.getKullanimYuzde());
        assertTrue(rapor.getSatirlar().isEmpty());
    }

    @Test
    void matris_kategoriAyDagiliminiVeToplamiHesaplar() {
        Butce p1 = Butce.builder().id(1L).sirketId(1L).yil(2026).ay(1).tur("GIDER")
                .kategori("Pazarlama").tutar(new BigDecimal("100")).build();
        Butce p2 = Butce.builder().id(2L).sirketId(1L).yil(2026).ay(2).tur("GIDER")
                .kategori("Pazarlama").tutar(new BigDecimal("200")).build();
        Butce k1 = Butce.builder().id(3L).sirketId(1L).yil(2026).ay(1).tur("GIDER")
                .kategori("Kira").tutar(new BigDecimal("50")).build();
        when(butceRepository.matrisSatirlari(1L, 2026, "GIDER"))
                .thenReturn(List.of(p1, p2, k1));

        var matris = butceService.matris(1L, 2026, "GIDER");

        assertEquals(2, matris.getSatirlar().size());
        var pazarlama = matris.getSatirlar().stream()
                .filter(s -> "Pazarlama".equals(s.getKategori())).findFirst().orElseThrow();
        assertEquals(0, pazarlama.getAylar()[0].compareTo(new BigDecimal("100")));
        assertEquals(0, pazarlama.getAylar()[1].compareTo(new BigDecimal("200")));
        assertEquals(0, pazarlama.getToplam().compareTo(new BigDecimal("300.00")));
        assertEquals(0, matris.getGenelToplam().compareTo(new BigDecimal("350.00")));
        assertEquals(0, matris.getAylikToplam()[0].compareTo(new BigDecimal("150")));
    }

    @Test
    void kopyala_mevcutKayitlariAtlarVeYenileriYazar() {
        Butce kaynak1 = Butce.builder().id(1L).sirketId(1L).yil(2025).ay(1).tur("GIDER")
                .kategori("Pazarlama").ad("2025 Pazarlama").tutar(new BigDecimal("100")).build();
        Butce kaynak2 = Butce.builder().id(2L).sirketId(1L).yil(2025).ay(2).tur("GIDER")
                .kategori("Pazarlama").ad("2025 Pazarlama").tutar(new BigDecimal("200")).build();
        Butce hedefMevcut = Butce.builder().id(3L).sirketId(1L).yil(2026).ay(1).tur("GIDER")
                .kategori("Pazarlama").tutar(new BigDecimal("999")).build();
        when(butceRepository.findBySirketIdAndYilAndTurOrderByKategoriAscAyAsc(1L, 2025, "GIDER"))
                .thenReturn(List.of(kaynak1, kaynak2));
        when(butceRepository.findBySirketIdAndYilAndTurOrderByKategoriAscAyAsc(1L, 2026, "GIDER"))
                .thenReturn(List.of(hedefMevcut));

        var sonuc = butceService.kopyala(1L, 2025, 2026, "GIDER");

        assertEquals(1, sonuc.get("kopyalanan"));
        assertEquals(1, sonuc.get("atlanan"));
        verify(butceRepository, times(1)).save(any(Butce.class));
    }

    @Test
    void kopyala_ayniYilReddedilir() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> butceService.kopyala(1L, 2026, 2026, null));
    }

    @Test
    void onayla_taslakButceyiOnaylarVeLogYazar() {
        Butce butce = Butce.builder().id(1L).sirketId(1L).ad("Bütçe").yil(2026).ay(1)
                .tutar(BigDecimal.TEN).tur("GIDER").durum("TASLAK").revizyon(2).build();
        when(butceRepository.findById(1L)).thenReturn(Optional.of(butce));
        when(butceRepository.save(any(Butce.class))).thenAnswer(inv -> inv.getArgument(0));

        var sonuc = butceService.onayla(1L, 7L);

        assertEquals("ONAYLANDI", sonuc.getDurum());
        assertEquals(7L, sonuc.getOnaylayanKullaniciId());
        assertNotNull(sonuc.getOnayTarihi());
        verify(butceRevizyonRepository).save(argThat(r -> "ONAY".equals(r.getIslem())));
    }

    @Test
    void reddet_gerekceyiKaydeder() {
        Butce butce = Butce.builder().id(1L).sirketId(1L).ad("Bütçe").yil(2026).ay(1)
                .tutar(BigDecimal.TEN).tur("GIDER").durum("TASLAK").revizyon(1).build();
        when(butceRepository.findById(1L)).thenReturn(Optional.of(butce));
        when(butceRepository.save(any(Butce.class))).thenAnswer(inv -> inv.getArgument(0));

        var sonuc = butceService.reddet(1L, "Tutar fazla", 7L);

        assertEquals("REDDEDILDI", sonuc.getDurum());
        assertEquals("Tutar fazla", sonuc.getRedGerekcesi());
        verify(butceRevizyonRepository).save(argThat(r -> "RED".equals(r.getIslem())));
    }

    @Test
    void onayla_onaylanmisButceyiReddeder() {
        Butce butce = ornekButce(1L);
        butce.setDurum("ONAYLANDI");
        when(butceRepository.findById(1L)).thenReturn(Optional.of(butce));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> butceService.onayla(1L, 7L));
    }

    @Test
    void revizyonlar_gecmisiDoner() {
        Butce butce = ornekButce(1L);
        when(butceRepository.findById(1L)).thenReturn(Optional.of(butce));
        when(butceRevizyonRepository.findByButceIdOrderByOlusturmaTarihiAscIdAsc(1L))
                .thenReturn(List.of(com.raspel.erp.entity.finans.ButceRevizyon.builder()
                        .id(1L).butceId(1L).revizyon(1).islem("OLUSTUR").durum("TASLAK")
                        .tutar(new BigDecimal("50000")).build()));

        var liste = butceService.revizyonlar(1L);

        assertEquals(1, liste.size());
        assertEquals("OLUSTUR", liste.get(0).getIslem());
    }
}
