package com.raspel.erp.service.sistem;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sistem.KitaplikDTO;
import com.raspel.erp.entity.sistem.Kitaplik;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.KitaplikRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KitaplikServiceTest {

    private static final Long SIRKET = 1L;
    private static final Long KULLANICI = 10L;
    private static final Long ADMIN = 99L;

    @Mock
    private KitaplikRepository repo;
    @Mock
    private TenantChecker tenantChecker;

    private KitaplikService servis;

    @BeforeEach
    void hazirla() {
        servis = new KitaplikService(repo, tenantChecker);
        lenient().doNothing().when(tenantChecker).check(any(), anyString());
        lenient().when(repo.save(any(Kitaplik.class))).thenAnswer(i -> i.getArgument(0));
    }

    private KitaplikDTO yerImi(String baslik, String url) {
        return KitaplikDTO.builder().tur("YER_IMI").baslik(baslik).url(url).build();
    }

    private KitaplikDTO snippet(String baslik, String icerik) {
        return KitaplikDTO.builder().tur("SNIPPET").baslik(baslik).icerik(icerik).build();
    }

    private Kitaplik kayit(Long id, String tur, String kapsam, String gorunurluk, Long sahip) {
        return Kitaplik.builder()
                .id(id).sirketId(SIRKET).tur(tur).kapsam(kapsam).gorunurluk(gorunurluk)
                .kullaniciId(sahip).baslik("Kayit-" + id).aktif(true)
                .olusturmaTarihi(LocalDateTime.now())
                .build();
    }

    // ---------------- olusturma ----------------

    @Test
    void kisiselYerImiOlusturulur() {
        when(repo.existsBySirketIdAndKullaniciIdAndTurAndBaslikIgnoreCaseAndAktifTrue(
                anyLong(), anyLong(), anyString(), anyString())).thenReturn(false);
        var dto = servis.olustur(yerImi("Netsis", "https://netsis.local"), SIRKET, KULLANICI, false);
        assertEquals("YER_IMI", dto.getTur());
        assertEquals("KISISEL", dto.getKapsam());
        assertEquals("SAHIS", dto.getGorunurluk());
    }

    @Test
    void normalKullaniciGlobalKayitYazamaz() {
        var dto = yerImi("Global", "https://x.local");
        dto.setKapsam("GLOBAL");
        assertThrows(BusinessException.class, () -> servis.olustur(dto, SIRKET, KULLANICI, false));
        verify(repo, never()).save(any());
    }

    @Test
    void adminGlobalTumuKayitYazabilir() {
        when(repo.existsBySirketIdAndKapsamAndTurAndBaslikIgnoreCaseAndAktifTrue(
                anyLong(), anyString(), anyString(), anyString())).thenReturn(false);
        var dto = snippet("Ortak Snippet", "echo merhaba");
        dto.setKapsam("GLOBAL");
        dto.setGorunurluk("TUMU");
        var sonuc = servis.olustur(dto, SIRKET, ADMIN, true);
        assertEquals("GLOBAL", sonuc.getKapsam());
        assertEquals("TUMU", sonuc.getGorunurluk());
    }

    @Test
    void yerImiIcinUrlZorunlu() {
        var dto = yerImi("Url'siz", null);
        assertThrows(BusinessException.class, () -> servis.olustur(dto, SIRKET, KULLANICI, false));
    }

    @Test
    void snippetIcinIcerikZorunlu() {
        var dto = snippet("Iceriksiz", null);
        assertThrows(BusinessException.class, () -> servis.olustur(dto, SIRKET, KULLANICI, false));
    }

    @Test
    void tumuGorunurlukYalnizGlobalKapsamda() {
        var dto = yerImi("X", "https://x.local");
        dto.setKapsam("KISISEL");
        dto.setGorunurluk("TUMU");
        assertThrows(BusinessException.class, () -> servis.olustur(dto, SIRKET, KULLANICI, false));
    }

    @Test
    void gecersizTurReddedilir() {
        var dto = KitaplikDTO.builder().tur("DOSYA").baslik("X").build();
        assertThrows(BusinessException.class, () -> servis.olustur(dto, SIRKET, KULLANICI, false));
    }

    @Test
    void gecersizKategoriReddedilir() {
        var dto = yerImi("X", "https://x.local");
        dto.setKategori("UYGUNSUZ");
        assertThrows(BusinessException.class, () -> servis.olustur(dto, SIRKET, KULLANICI, false));
    }

    @Test
    void ayniBaslikIkiKezEklenemez() {
        when(repo.existsBySirketIdAndKullaniciIdAndTurAndBaslikIgnoreCaseAndAktifTrue(
                SIRKET, KULLANICI, "YER_IMI", "Netsis")).thenReturn(true);
        assertThrows(BusinessException.class,
                () -> servis.olustur(yerImi("Netsis", "https://x"), SIRKET, KULLANICI, false));
    }

    @Test
    void sirketsizIstekReddedilir() {
        assertThrows(BusinessException.class,
                () -> servis.olustur(yerImi("X", "https://x"), null, KULLANICI, false));
    }

    // ---------------- gorunurluk / okuma ----------------

    @Test
    void listele_adminBayraginiRepoyaGecirir() {
        when(repo.gorunurKayitlar(eq(SIRKET), eq(KULLANICI), eq(false), any(), any(), any(), any()))
                .thenReturn(List.of(kayit(1L, "YER_IMI", "KISISEL", "SAHIS", KULLANICI)));
        var liste = servis.listele(SIRKET, KULLANICI, false, null, true, null, null);
        assertEquals(1, liste.size());
        verify(repo).gorunurKayitlar(eq(SIRKET), eq(KULLANICI), eq(false), isNull(), eq(true), isNull(), isNull());
    }

    @Test
    void kisiselKaydiBaskasiGoremez() {
        when(repo.findById(1L)).thenReturn(Optional.of(kayit(1L, "YER_IMI", "KISISEL", "SAHIS", KULLANICI)));
        assertThrows(ResourceNotFoundException.class,
                () -> servis.idyeGoreGetir(1L, SIRKET, 55L, false));
    }

    @Test
    void globalSahisKaydiYalnizAdminGorur() {
        when(repo.findById(2L)).thenReturn(Optional.of(kayit(2L, "SNIPPET", "GLOBAL", "SAHIS", null)));
        assertThrows(ResourceNotFoundException.class,
                () -> servis.idyeGoreGetir(2L, SIRKET, 55L, false));
        // ADMIN gorebilir
        assertNotNull(servis.idyeGoreGetir(2L, SIRKET, ADMIN, true));
    }

    @Test
    void globalTumuKaydiHerkesGorur() {
        when(repo.findById(3L)).thenReturn(Optional.of(kayit(3L, "SNIPPET", "GLOBAL", "TUMU", null)));
        assertNotNull(servis.idyeGoreGetir(3L, SIRKET, 55L, false));
    }

    // ---------------- guncelleme / yetki ----------------

    @Test
    void kisiselKaydiSahibiGunceller() {
        var k = kayit(1L, "YER_IMI", "KISISEL", "SAHIS", KULLANICI);
        k.setUrl("https://eski");
        when(repo.findById(1L)).thenReturn(Optional.of(k));
        var dto = KitaplikDTO.builder().baslik("Yeni Baslik").url("https://yeni").build();
        var sonuc = servis.guncelle(1L, dto, SIRKET, KULLANICI, false);
        assertEquals("Yeni Baslik", sonuc.getBaslik());
        assertEquals("https://yeni", sonuc.getUrl());
    }

    @Test
    void kisiselKaydiBaskasiGuncelleyemez() {
        when(repo.findById(1L)).thenReturn(Optional.of(kayit(1L, "YER_IMI", "KISISEL", "SAHIS", KULLANICI)));
        assertThrows(ResourceNotFoundException.class,
                () -> servis.guncelle(1L, yerImi("X", "https://x"), SIRKET, 55L, false));
    }

    @Test
    void globalKaydiYalnizAdminGunceller() {
        when(repo.findById(2L)).thenReturn(Optional.of(kayit(2L, "SNIPPET", "GLOBAL", "TUMU", null)));
        assertThrows(BusinessException.class,
                () -> servis.guncelle(2L, snippet("Yeni", "kod"), SIRKET, 55L, false));
    }

    @Test
    void yerImiGuncellemedeUrlBosBirakilirsaReddedilir() {
        // Mevcut url null ise guncelleme sonrasi da url yoksa kural ihlali.
        var k = kayit(1L, "YER_IMI", "KISISEL", "SAHIS", KULLANICI);
        k.setUrl(null);
        when(repo.findById(1L)).thenReturn(Optional.of(k));
        var dto = KitaplikDTO.builder().baslik("Baslik").build();
        assertThrows(BusinessException.class, () -> servis.guncelle(1L, dto, SIRKET, KULLANICI, false));
    }

    // ---------------- arsiv ----------------

    @Test
    void arsivlemeFizikselSilmez() {
        var k = kayit(1L, "YER_IMI", "KISISEL", "SAHIS", KULLANICI);
        when(repo.findById(1L)).thenReturn(Optional.of(k));
        servis.arsivle(1L, SIRKET, KULLANICI, false);
        verify(repo, never()).deleteById(anyLong());
        verify(repo, never()).delete(any());
        assertFalse(k.getAktif());
        assertNotNull(k.getArsivTarihi());
    }

    @Test
    void arsivdenGeriAlmaAktifler() {
        var k = kayit(1L, "YER_IMI", "KISISEL", "SAHIS", KULLANICI);
        k.setAktif(false);
        k.setArsivTarihi(LocalDateTime.now());
        when(repo.findById(1L)).thenReturn(Optional.of(k));
        servis.arsivleGeriAl(1L, SIRKET, KULLANICI, false);
        assertTrue(k.getAktif());
        assertNull(k.getArsivTarihi());
    }

    @Test
    void baskaSirketKaydiBulunamaz() {
        var k = Kitaplik.builder().id(1L).sirketId(999L).tur("YER_IMI").kapsam("KISISEL")
                .gorunurluk("SAHIS").baslik("X").aktif(true).build();
        when(repo.findById(1L)).thenReturn(Optional.of(k));
        doThrow(new ResourceNotFoundException("Kitaplik bu sirkete ait degil"))
                .when(tenantChecker).check(anyLong(), anyString());
        assertThrows(ResourceNotFoundException.class,
                () -> servis.idyeGoreGetir(1L, SIRKET, KULLANICI, false));
    }
}