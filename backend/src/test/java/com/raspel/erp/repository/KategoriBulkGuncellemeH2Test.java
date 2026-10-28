package com.raspel.erp.repository;

import com.raspel.erp.dto.sistem.KategoriDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.finans.Butce;
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.entity.finans.KasaHareket;
import com.raspel.erp.entity.finans.Masraf;
import com.raspel.erp.entity.sistem.Kategori;
import com.raspel.erp.entity.sistem.KategoriKapsam;
import com.raspel.erp.entity.ticaret.IskontoKurali;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.ButceRepository;
import com.raspel.erp.repository.finans.KasaHareketRepository;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.finans.MasrafRepository;
import com.raspel.erp.repository.sistem.KategoriRepository;
import com.raspel.erp.repository.ticaret.IskontoKuraliRepository;
import com.raspel.erp.service.sistem.KategoriService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kategori yeniden adlandırma ve birleştirmenin 5 modüldeki legacy string
 * kolonlara ve {@code kategori_id} bağlantılarına gerçekten yayıldığını
 * doğrular. Bulk JPQL update'leri Mockito ile test edilemez; bu test H2
 * üzerinde sorguları fiilen çalıştırır.
 */
@SpringBootTest
class KategoriBulkGuncellemeH2Test {

    private static final Long STOK_SIRKET = 9811L;
    private static final Long MASRAF_SIRKET = 9812L;
    private static final Long BUTCE_SIRKET = 9813L;
    private static final Long KASA_SIRKET = 9814L;

    @Autowired private KategoriRepository kategoriRepository;
    @Autowired private StokRepository stokRepository;
    @Autowired private MasrafRepository masrafRepository;
    @Autowired private ButceRepository butceRepository;
    @Autowired private IskontoKuraliRepository iskontoKuraliRepository;
    @Autowired private KasaRepository kasaRepository;
    @Autowired private KasaHareketRepository kasaHareketRepository;
    @Autowired private KategoriService kategoriService;

    private Kategori kategori(Long sirketId, KategoriKapsam kapsam, String ad) {
        return kategoriRepository.save(Kategori.builder()
                .sirketId(sirketId).kapsam(kapsam).ad(ad).sira(0).aktif(true).build());
    }

    /**
     * TenantChecker fail-closed çalışır; @SpringBootTest'te istek bağlamı vardır
     * ama sirketId attribute'u yoktur. Testte aktif şirket burada seçilir.
     */
    private void sirketSec(Long sirketId) {
        var attrs = (org.springframework.web.context.request.ServletRequestAttributes)
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        assertNotNull(attrs, "Test istek bağlamı bekleniyordu");
        attrs.getRequest().setAttribute("sirketId", sirketId);
    }

    @Test
    void stokVeIskonto_renameVeMergeYayilir() {
        sirketSec(STOK_SIRKET);
        Kategori kaynak = kategori(STOK_SIRKET, KategoriKapsam.STOK, "ELEKTRONIK");
        Kategori hedef = kategori(STOK_SIRKET, KategoriKapsam.STOK, "TEKNOLOJI");

        Stok stok = stokRepository.save(Stok.builder()
                .stokKodu("KAT-1").ad("Telefon").birim("ADET")
                .fiyat(BigDecimal.TEN).miktar(BigDecimal.ONE)
                .kategori("ELEKTRONIK").kategoriId(kaynak.getId())
                .sirketId(STOK_SIRKET).build());
        iskontoKuraliRepository.save(IskontoKurali.builder()
                .sirketId(STOK_SIRKET).ad("Kural").kategori("ELEKTRONIK")
                .kategoriId(kaynak.getId()).iskontoOrani(BigDecimal.TEN).build());

        // 1) Yeniden adlandırma legacy kolonlara yayılır.
        kategoriService.guncelle(kaynak.getId(), KategoriDTO.builder().ad("ELEKTRO").build());
        assertEquals("ELEKTRO", stokRepository.findById(stok.getId()).orElseThrow().getKategori());
        assertEquals("ELEKTRO",
                iskontoKuraliRepository.findAll().stream()
                        .filter(k -> k.getKategoriId() != null && k.getKategoriId().equals(kaynak.getId()))
                        .findFirst().orElseThrow().getKategori());

        // 2) Birleştirme tüm bağları hedefe taşır.
        kategoriService.birlestir(kaynak.getId(), hedef.getId());

        Stok tasinan = stokRepository.findById(stok.getId()).orElseThrow();
        assertEquals(hedef.getId(), tasinan.getKategoriId());
        assertEquals("TEKNOLOJI", tasinan.getKategori());
        assertTrue(iskontoKuraliRepository.findAll().stream()
                .filter(k -> k.getKategoriId() != null)
                .anyMatch(k -> k.getKategoriId().equals(hedef.getId()) && "TEKNOLOJI".equals(k.getKategori())));
        assertEquals(0L, stokRepository.countByKategoriId(kaynak.getId()));

        // 3) Kaynak arşivlenir.
        assertFalse(kategoriRepository.findById(kaynak.getId()).orElseThrow().getAktif());

        // 4) Kullanım sayımı hedefte 2 kayıt (stok + iskonto) gösterir.
        var kullanim = kategoriService.kullanim(hedef.getId());
        assertEquals(2, kullanim.getToplam());
    }

    @Test
    void masrafRenameLegacyKolonuBuyukHarfGunceller() {
        sirketSec(MASRAF_SIRKET);
        Kategori kategori = kategori(MASRAF_SIRKET, KategoriKapsam.MASRAF, "KIRA");
        Masraf masraf = masrafRepository.save(Masraf.builder()
                .tarih(LocalDate.now()).tutar(BigDecimal.TEN)
                .kategori("KIRA").kategoriId(kategori.getId())
                .sirketId(MASRAF_SIRKET).build());

        kategoriService.guncelle(kategori.getId(), KategoriDTO.builder().ad(" ofis kirası ").build());

        Masraf guncel = masrafRepository.findById(masraf.getId()).orElseThrow();
        assertEquals("OFIS KIRASI", guncel.getKategori());
        assertEquals(kategori.getId(), guncel.getKategoriId());
    }

    @Test
    void butceMergeLegacyKolonuHedefeTasir() {
        sirketSec(BUTCE_SIRKET);
        Kategori kaynak = kategori(BUTCE_SIRKET, KategoriKapsam.BUTCE, "PAZARLAMA");
        Kategori hedef = kategori(BUTCE_SIRKET, KategoriKapsam.BUTCE, "SATIS");
        Butce butce = butceRepository.save(Butce.builder()
                .ad("2026 Bütçe").yil(2026).ay(1).tutar(BigDecimal.TEN).tur("GIDER")
                .kategori("PAZARLAMA").kategoriId(kaynak.getId()).sirketId(BUTCE_SIRKET).build());

        kategoriService.birlestir(kaynak.getId(), hedef.getId());

        Butce tasinan = butceRepository.findById(butce.getId()).orElseThrow();
        assertEquals(hedef.getId(), tasinan.getKategoriId());
        assertEquals("SATIS", tasinan.getKategori());
    }

    @Test
    void kasaHareketiMergeIleHedefKategoriyeTasinir() {
        sirketSec(KASA_SIRKET);
        Kategori kaynak = kategori(KASA_SIRKET, KategoriKapsam.KASA_GELIR, "SATIS");
        Kategori hedef = kategori(KASA_SIRKET, KategoriKapsam.KASA_GELIR, "TAHSILAT");
        Kasa kasa = kasaRepository.save(Kasa.builder()
                .ad("Merkez").bakiye(BigDecimal.ZERO).sirketId(KASA_SIRKET).build());
        KasaHareket hareket = kasaHareketRepository.save(KasaHareket.builder()
                .kasa(kasa).tur("GELIR").tutar(BigDecimal.TEN)
                .hareketTarihi(LocalDate.now()).kategori(kaynak).build());

        kategoriService.birlestir(kaynak.getId(), hedef.getId());

        KasaHareket tasinan = kasaHareketRepository.findById(hareket.getId()).orElseThrow();
        assertEquals(hedef.getId(), tasinan.getKategori().getId());
        assertEquals(0L, kasaHareketRepository.countByKategoriId(kaynak.getId()));
    }
}
