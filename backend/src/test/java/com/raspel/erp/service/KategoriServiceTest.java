package com.raspel.erp.service;

import com.raspel.erp.dto.sistem.KategoriDTO;
import com.raspel.erp.dto.sistem.KategoriKullanimDTO;
import com.raspel.erp.entity.sistem.Kategori;
import com.raspel.erp.entity.sistem.KategoriKapsam;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.ButceRepository;
import com.raspel.erp.repository.finans.KasaHareketRepository;
import com.raspel.erp.repository.finans.MasrafRepository;
import com.raspel.erp.repository.sistem.KategoriRepository;
import com.raspel.erp.repository.ticaret.IskontoKuraliRepository;
import com.raspel.erp.service.sistem.KategoriService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KategoriServiceTest {

    @Mock private KategoriRepository kategoriRepository;
    @Mock private StokRepository stokRepository;
    @Mock private MasrafRepository masrafRepository;
    @Mock private ButceRepository butceRepository;
    @Mock private IskontoKuraliRepository iskontoKuraliRepository;
    @Mock private KasaHareketRepository kasaHareketRepository;
    @Mock private com.raspel.erp.config.TenantChecker tenantChecker;
    @InjectMocks private KategoriService kategoriService;

    private Kategori kategori(Long id, String ad, KategoriKapsam kapsam) {
        return kategori(id, ad, kapsam, null);
    }

    private Kategori kategori(Long id, String ad, KategoriKapsam kapsam, Kategori ust) {
        return Kategori.builder()
                .id(id).sirketId(1L).kapsam(kapsam).ad(ad)
                .ustKategori(ust).sira(0).aktif(true)
                .olusturmaTarihi(LocalDateTime.now())
                .build();
    }

    // ---------------- Okuma ----------------

    @Test
    void tumunuGetir_tumKapsamlariListeler() {
        when(kategoriRepository.listeleTumKapsamlar(eq(1L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        kategori(1L, "Kira", KategoriKapsam.MASRAF),
                        kategori(2L, "Elektronik", KategoriKapsam.STOK))));

        var sonuc = kategoriService.tumunuGetir(1L, Pageable.unpaged());

        assertEquals(2, sonuc.getContent().size());
        assertEquals("Kira", sonuc.getContent().get(0).getAd());
        assertEquals("MASRAF", sonuc.getContent().get(0).getKapsam());
    }

    @Test
    void listele_kapsamVeAramaParametreleriniIletir() {
        when(kategoriRepository.listele(eq(1L), eq(KategoriKapsam.STOK), eq("%elekt%"),
                eq(true), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(kategori(3L, "Elektronik", KategoriKapsam.STOK))));

        var sonuc = kategoriService.listele(1L, KategoriKapsam.STOK, "elekt", true, null, Pageable.unpaged());

        assertEquals(1, sonuc.getContent().size());
        assertEquals("Elektronik", sonuc.getContent().get(0).getAd());
    }

    @Test
    void agac_ikiSeviyeliAgacKurar() {
        Kategori ust = kategori(1L, "Gıda", KategoriKapsam.STOK);
        Kategori alt = kategori(2L, "Süt", KategoriKapsam.STOK, ust);
        when(kategoriRepository.findBySirketIdAndKapsamOrderBySiraAscAdAsc(1L, KategoriKapsam.STOK))
                .thenReturn(List.of(ust, alt));

        var agac = kategoriService.agac(1L, KategoriKapsam.STOK, null, null);

        assertEquals(1, agac.size());
        assertEquals("Gıda", agac.get(0).getAd());
        assertEquals(1, agac.get(0).getAltKategoriler().size());
        assertEquals("Süt", agac.get(0).getAltKategoriler().get(0).getAd());
        assertEquals(1L, agac.get(0).getAltKategoriSayisi());
    }

    @Test
    void agac_aramaAltKategoriyiUstuYoksaKokeTasir() {
        Kategori ust = kategori(1L, "Gıda", KategoriKapsam.STOK);
        Kategori alt = kategori(2L, "Süt", KategoriKapsam.STOK, ust);
        when(kategoriRepository.findBySirketIdAndKapsamOrderBySiraAscAdAsc(1L, KategoriKapsam.STOK))
                .thenReturn(List.of(ust, alt));

        var agac = kategoriService.agac(1L, KategoriKapsam.STOK, "süt", null);

        assertEquals(1, agac.size());
        assertEquals("Süt", agac.get(0).getAd());
    }

    @Test
    void secenekler_yalnizAktifleriDoner() {
        Kategori aktif = kategori(1L, "Gıda", KategoriKapsam.STOK);
        Kategori pasif = kategori(2L, "Eski", KategoriKapsam.STOK);
        pasif.setAktif(false);
        when(kategoriRepository.findBySirketIdAndKapsamOrderBySiraAscAdAsc(1L, KategoriKapsam.STOK))
                .thenReturn(List.of(aktif, pasif));

        var secenekler = kategoriService.secenekler(1L, KategoriKapsam.STOK);

        assertEquals(1, secenekler.size());
        assertEquals("Gıda", secenekler.get(0).getAd());
    }

    @Test
    void ozet_sayimlariDondurur() {
        when(kategoriRepository.countBySirketIdAndKapsam(1L, KategoriKapsam.STOK)).thenReturn(10L);
        when(kategoriRepository.countBySirketIdAndKapsamAndAktif(1L, KategoriKapsam.STOK, true)).thenReturn(7L);
        when(kategoriRepository.countBySirketIdAndKapsamAndUstKategoriIdIsNotNull(1L, KategoriKapsam.STOK))
                .thenReturn(3L);

        var ozet = kategoriService.ozet(1L, KategoriKapsam.STOK);

        assertEquals(10, ozet.getToplam());
        assertEquals(7, ozet.getAktif());
        assertEquals(3, ozet.getPasif());
        assertEquals(3, ozet.getAltKategori());
    }

    @Test
    void kullanim_stokModulunuSayar() {
        when(kategoriRepository.findById(1L))
                .thenReturn(Optional.of(kategori(1L, "Gıda", KategoriKapsam.STOK)));
        when(stokRepository.countByKategoriId(1L)).thenReturn(4L);
        when(iskontoKuraliRepository.countByKategoriId(1L)).thenReturn(1L);

        KategoriKullanimDTO kullanim = kategoriService.kullanim(1L);

        assertEquals(5, kullanim.getToplam());
        assertEquals(2, kullanim.getDetaylar().size());
        assertEquals("STOK", kullanim.getDetaylar().get(0).getModul());
    }

    @Test
    void sistemKategorileri_personelKodlariniDoner() {
        var liste = kategoriService.sistemKategorileri();
        assertEquals(6, liste.size());
        assertTrue(liste.stream().anyMatch(s -> "YAKIT".equals(s.getKod())));
    }

    @Test
    void turuGetir_gelirKapsaminiCozer() {
        when(kategoriRepository.findBySirketIdAndKapsamOrderBySiraAscAdAsc(1L, KategoriKapsam.KASA_GELIR))
                .thenReturn(List.of(kategori(1L, "Satış", KategoriKapsam.KASA_GELIR)));

        var liste = kategoriService.turuGetir("GELIR", 1L);

        assertEquals(1, liste.size());
        assertEquals("GELIR", liste.get(0).getTur());
    }

    // ---------------- CRUD + doğrulama ----------------

    @Test
    void olustur_kaydeder() {
        when(kategoriRepository.existsBySirketIdAndKapsamAndAdIgnoreCase(1L, KategoriKapsam.STOK, "Gıda"))
                .thenReturn(false);
        when(kategoriRepository.save(any(Kategori.class))).thenAnswer(inv -> {
            Kategori k = inv.getArgument(0);
            k.setId(9L);
            return k;
        });

        var dto = kategoriService.olustur(KategoriDTO.builder()
                .kapsam("STOK").ad("  Gıda  ").build(), 1L);

        assertEquals(9L, dto.getId());
        assertEquals("Gıda", dto.getAd());
        assertEquals("STOK", dto.getKapsam());
    }

    @Test
    void olustur_ayniAdReddedilir() {
        when(kategoriRepository.existsBySirketIdAndKapsamAndAdIgnoreCase(1L, KategoriKapsam.STOK, "Gıda"))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> kategoriService.olustur(
                KategoriDTO.builder().kapsam("STOK").ad("Gıda").build(), 1L));
    }

    @Test
    void olustur_masrafAdiBuyukHarfOlur() {
        when(kategoriRepository.save(any(Kategori.class))).thenAnswer(inv -> {
            Kategori k = inv.getArgument(0);
            k.setId(2L);
            return k;
        });

        var dto = kategoriService.olustur(KategoriDTO.builder()
                .kapsam("MASRAF").ad(" ofis ").build(), 1L);

        assertEquals("OFIS", dto.getAd());
    }

    @Test
    void olustur_gecersizRenkReddedilir() {
        assertThrows(BusinessException.class, () -> kategoriService.olustur(KategoriDTO.builder()
                .kapsam("STOK").ad("Gıda").renk("#123456").build(), 1L));
    }

    @Test
    void olustur_gecersizIkonReddedilir() {
        assertThrows(BusinessException.class, () -> kategoriService.olustur(KategoriDTO.builder()
                .kapsam("STOK").ad("Gıda").ikon("box").build(), 1L));
    }

    @Test
    void olustur_negatifSiraReddedilir() {
        assertThrows(BusinessException.class, () -> kategoriService.olustur(KategoriDTO.builder()
                .kapsam("STOK").ad("Gıda").sira(-1).build(), 1L));
    }

    @Test
    void olustur_ucuncuSeviyeReddedilir() {
        Kategori kok = kategori(1L, "Gıda", KategoriKapsam.STOK);
        Kategori alt = kategori(2L, "Süt", KategoriKapsam.STOK, kok);
        when(kategoriRepository.findById(2L)).thenReturn(Optional.of(alt));

        assertThrows(BusinessException.class, () -> kategoriService.olustur(KategoriDTO.builder()
                .kapsam("STOK").ad("Tam Yağlı").ustKategoriId(2L).build(), 1L));
    }

    @Test
    void olustur_farkliKapsamUstKategoriReddedilir() {
        Kategori ust = kategori(1L, "Kira", KategoriKapsam.MASRAF);
        when(kategoriRepository.findById(1L)).thenReturn(Optional.of(ust));

        assertThrows(BusinessException.class, () -> kategoriService.olustur(KategoriDTO.builder()
                .kapsam("STOK").ad("Gıda").ustKategoriId(1L).build(), 1L));
    }

    @Test
    void guncelle_adDegisimiLegacyKolonlaraYayilir() {
        Kategori mevcut = kategori(1L, "Elektronik", KategoriKapsam.STOK);
        when(kategoriRepository.findById(1L)).thenReturn(Optional.of(mevcut));
        when(kategoriRepository.findBySirketIdAndKapsamAndAdIgnoreCaseAndIdNot(
                1L, KategoriKapsam.STOK, "Teknoloji", 1L)).thenReturn(Optional.empty());
        when(kategoriRepository.save(any(Kategori.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = kategoriService.guncelle(1L, KategoriDTO.builder().ad("Teknoloji").build());

        assertEquals("Teknoloji", dto.getAd());
        verify(stokRepository).kategoriAdGuncelle(1L, "Teknoloji");
        verify(iskontoKuraliRepository).kategoriAdGuncelle(1L, "Teknoloji");
    }

    @Test
    void guncelle_kendineBaglanamaz() {
        Kategori mevcut = kategori(1L, "Gıda", KategoriKapsam.STOK);
        when(kategoriRepository.findById(1L)).thenReturn(Optional.of(mevcut));

        assertThrows(BusinessException.class, () -> kategoriService.guncelle(1L,
                KategoriDTO.builder().ad("Gıda").ustKategoriId(1L).build()));
    }

    @Test
    void guncelle_kullanilanKategorininKapsamiDegismez() {
        Kategori mevcut = kategori(1L, "Gıda", KategoriKapsam.STOK);
        when(kategoriRepository.findById(1L)).thenReturn(Optional.of(mevcut));
        when(stokRepository.countByKategoriId(1L)).thenReturn(2L);

        assertThrows(BusinessException.class, () -> kategoriService.guncelle(1L,
                KategoriDTO.builder().kapsam("MASRAF").build()));
    }

    @Test
    void sil_kullanimVarsaReddedilir() {
        when(kategoriRepository.findById(1L))
                .thenReturn(Optional.of(kategori(1L, "Gıda", KategoriKapsam.STOK)));
        when(stokRepository.countByKategoriId(1L)).thenReturn(3L);

        var hata = assertThrows(BusinessException.class, () -> kategoriService.sil(1L));
        assertTrue(hata.getMessage().contains("3 kayıtta kullanılıyor"));
        verify(kategoriRepository, never()).delete(any());
    }

    @Test
    void sil_altKategoriVarsaReddedilir() {
        when(kategoriRepository.findById(1L))
                .thenReturn(Optional.of(kategori(1L, "Gıda", KategoriKapsam.STOK)));
        when(kategoriRepository.findByUstKategoriId(1L))
                .thenReturn(List.of(kategori(2L, "Süt", KategoriKapsam.STOK)));

        assertThrows(BusinessException.class, () -> kategoriService.sil(1L));
    }

    @Test
    void sil_kullanilmiyorsaSiler() {
        Kategori k = kategori(1L, "Gıda", KategoriKapsam.STOK);
        when(kategoriRepository.findById(1L)).thenReturn(Optional.of(k));

        kategoriService.sil(1L);

        verify(kategoriRepository).delete(k);
    }

    @Test
    void sil_bulunamazsaHataFirlatir() {
        when(kategoriRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> kategoriService.sil(99L));
    }

    @Test
    void arsivle_aktifAltKategoriVarsaReddedilir() {
        when(kategoriRepository.findById(1L))
                .thenReturn(Optional.of(kategori(1L, "Gıda", KategoriKapsam.STOK)));
        when(kategoriRepository.countByUstKategoriIdAndAktifTrue(1L)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> kategoriService.arsivle(1L));
    }

    @Test
    void arsivle_aktifFalseYapar() {
        Kategori k = kategori(1L, "Gıda", KategoriKapsam.STOK);
        when(kategoriRepository.findById(1L)).thenReturn(Optional.of(k));
        when(kategoriRepository.save(any(Kategori.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = kategoriService.arsivle(1L);

        assertFalse(dto.getAktif());
        verify(kategoriRepository).countByUstKategoriIdAndAktifTrue(1L);
    }

    @Test
    void aktiflestir_pasifUstVarsaReddedilir() {
        Kategori ust = kategori(1L, "Gıda", KategoriKapsam.STOK);
        ust.setAktif(false);
        Kategori alt = kategori(2L, "Süt", KategoriKapsam.STOK, ust);
        alt.setAktif(false);
        when(kategoriRepository.findById(2L)).thenReturn(Optional.of(alt));

        assertThrows(BusinessException.class, () -> kategoriService.aktiflestir(2L));
    }

    @Test
    void aktiflestir_aktifTrueYapar() {
        Kategori k = kategori(1L, "Gıda", KategoriKapsam.STOK);
        k.setAktif(false);
        when(kategoriRepository.findById(1L)).thenReturn(Optional.of(k));
        when(kategoriRepository.save(any(Kategori.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = kategoriService.aktiflestir(1L);

        assertTrue(dto.getAktif());
    }

    @Test
    void birlestir_farkliKapsamReddedilir() {
        when(kategoriRepository.findById(1L))
                .thenReturn(Optional.of(kategori(1L, "Gıda", KategoriKapsam.STOK)));
        when(kategoriRepository.findById(2L))
                .thenReturn(Optional.of(kategori(2L, "Kira", KategoriKapsam.MASRAF)));

        assertThrows(BusinessException.class, () -> kategoriService.birlestir(1L, 2L));
    }

    @Test
    void birlestir_ayniKategoriReddedilir() {
        assertThrows(BusinessException.class, () -> kategoriService.birlestir(1L, 1L));
    }

    @Test
    void birlestir_baglariTasirVeKaynagiArsivler() {
        Kategori kaynak = kategori(1L, "Elektronik", KategoriKapsam.STOK);
        Kategori hedef = kategori(2L, "Teknoloji", KategoriKapsam.STOK);
        Kategori cocuk = kategori(3L, "Telefon", KategoriKapsam.STOK, kaynak);
        when(kategoriRepository.findById(1L)).thenReturn(Optional.of(kaynak));
        when(kategoriRepository.findById(2L)).thenReturn(Optional.of(hedef));
        when(kategoriRepository.findByUstKategoriId(1L)).thenReturn(List.of(cocuk));
        when(kategoriRepository.save(any(Kategori.class))).thenAnswer(inv -> inv.getArgument(0));

        kategoriService.birlestir(1L, 2L);

        verify(stokRepository).kategoriBirlestir(1L, 2L, "Teknoloji");
        verify(iskontoKuraliRepository).kategoriBirlestir(1L, 2L, "Teknoloji");
        assertEquals(hedef, cocuk.getUstKategori());
        assertFalse(kaynak.getAktif());
    }

    @Test
    void birlestir_pasifHedefReddedilir() {
        Kategori kaynak = kategori(1L, "A", KategoriKapsam.STOK);
        Kategori hedef = kategori(2L, "B", KategoriKapsam.STOK);
        hedef.setAktif(false);
        when(kategoriRepository.findById(1L)).thenReturn(Optional.of(kaynak));
        when(kategoriRepository.findById(2L)).thenReturn(Optional.of(hedef));

        assertThrows(BusinessException.class, () -> kategoriService.birlestir(1L, 2L));
    }

    // ---------------- resolve (get-or-create) ----------------

    @Test
    void resolve_varOlanKategoriyiDoner() {
        Kategori mevcut = kategori(5L, "GIDA", KategoriKapsam.MASRAF);
        when(kategoriRepository.findBySirketIdAndKapsamAndAdIgnoreCase(1L, KategoriKapsam.MASRAF, "GIDA"))
                .thenReturn(Optional.of(mevcut));

        var sonuc = kategoriService.resolve(1L, KategoriKapsam.MASRAF, " gida ");

        assertEquals(5L, sonuc.getId());
        verify(kategoriRepository, never()).save(any());
    }

    @Test
    void resolve_yoksaOlustururVeBuyukHarfNormalizeEder() {
        when(kategoriRepository.findBySirketIdAndKapsamAndAdIgnoreCase(1L, KategoriKapsam.MASRAF, "KIRA"))
                .thenReturn(Optional.empty());
        when(kategoriRepository.save(any(Kategori.class))).thenAnswer(inv -> {
            Kategori k = inv.getArgument(0);
            k.setId(7L);
            return k;
        });

        var sonuc = kategoriService.resolve(1L, KategoriKapsam.MASRAF, "kira");

        assertEquals("KIRA", sonuc.getAd());
        assertEquals(7L, sonuc.getId());
        assertEquals(KategoriKapsam.MASRAF, sonuc.getKapsam());
    }

    @Test
    void resolve_bosAdIcinNullDoner() {
        assertNull(kategoriService.resolve(1L, KategoriKapsam.STOK, "   "));
        assertNull(kategoriService.resolve(1L, KategoriKapsam.STOK, null));
        verifyNoInteractions(kategoriRepository);
    }
}
