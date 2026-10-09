package com.raspel.erp.service.sistem;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sistem.SifreKasaDTO;
import com.raspel.erp.dto.sistem.SifreKasaOzetDTO;
import com.raspel.erp.entity.sistem.SifreKasa;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.SifreKasaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SifreKasaServiceTest {

    private static final Long SIRKET = 1L;
    private static final Long KULLANICI = 10L;
    private static final Long ADMIN = 99L;
    private static final String ANAHTAR = "kasa-test-anahtari-en-az-16-karakter";

    @Mock
    private SifreKasaRepository repo;

    @Mock
    private TenantChecker tenantChecker;

    @Mock
    private AuditLogService auditLogService;

    private SifreKasaService servis;

    @BeforeEach
    void hazirla() {
        servis = new SifreKasaService(repo, tenantChecker, auditLogService);
        ReflectionTestUtils.setField(servis, "encryptionKey", ANAHTAR);
        // TenantChecker request baglami yoksa kontrolu gecsin (unit test).
        lenient().doNothing().when(tenantChecker).check(any(), anyString());
    }

    // ---------------- yardimcilar ----------------

    private SifreKasaDTO form(String baslik, String sifre) {
        return SifreKasaDTO.builder().baslik(baslik).sifre(sifre).build();
    }

    private SifreKasa kayit(Long id, String kapsam, String gorunurluk, Long sahip) {
        return SifreKasa.builder()
                .id(id).sirketId(SIRKET).kapsam(kapsam).sifreGorunurlugu(gorunurluk)
                .kullaniciId(sahip).baslik("Kayit-" + id).sifreCipher("x")
                .aktif(true).olusturmaTarihi(LocalDateTime.now())
                .build();
    }

    private SifreKasa kaydetVeDon(SifreKasa k) {
        when(repo.save(any(SifreKasa.class))).thenAnswer(i -> i.getArgument(0));
        return k;
    }

    // ============================================================
    // 1) SIFRELEME
    // ============================================================

    @Test
    void olusturma_sifreDuzMetinSaklanmaz() {
        when(repo.existsBySirketIdAndBaslikIgnoreCaseAndAktifTrue(anyLong(), anyString())).thenReturn(false);
        kaydetVeDon(null);

        servis.olustur(form("Netsis", "CokGizli123!"), SIRKET, KULLANICI, false);

        ArgumentCaptor<SifreKasa> cap = ArgumentCaptor.forClass(SifreKasa.class);
        verify(repo).save(cap.capture());
        String cipher = cap.getValue().getSifreCipher();
        assertNotNull(cipher);
        assertNotEquals("CokGizli123!", cipher, "sifre duz metin saklanmamali");
        assertFalse(cipher.contains("Gizli"), "sifre duz metin izi tasimamali");
        assertTrue(cipher.matches("^[A-Za-z0-9+/=]+$"), "cipher base64 olmali");
    }

    @Test
    void sifre_gidisDonusDogruMetniVerir() {
        when(repo.existsBySirketIdAndBaslikIgnoreCaseAndAktifTrue(anyLong(), anyString())).thenReturn(false);
        kaydetVeDon(null);
        ArgumentCaptor<SifreKasa> cap = ArgumentCaptor.forClass(SifreKasa.class);
        servis.olustur(form("Netsis", "CokGizli123!"), SIRKET, KULLANICI, false);
        verify(repo).save(cap.capture());

        String duz = ReflectionTestUtils.invokeMethod(servis, "coz", cap.getValue().getSifreCipher());
        assertEquals("CokGizli123!", duz);
    }

    @Test
    void ikiAyniKayitFarkliCipherUretir() {
        // Ayni sifre iki kez kaydedilse bile cipher farkli olmali
        // (rastgele IV sayesinde; aksi halde ayni sifre tespit edilebilirdi).
        when(repo.existsBySirketIdAndBaslikIgnoreCaseAndAktifTrue(anyLong(), anyString())).thenReturn(false);
        kaydetVeDon(null);
        ArgumentCaptor<SifreKasa> cap = ArgumentCaptor.forClass(SifreKasa.class);

        servis.olustur(form("A", "AyniSifre"), SIRKET, KULLANICI, false);
        servis.olustur(form("B", "AyniSifre"), SIRKET, KULLANICI, false);
        verify(repo, times(2)).save(cap.capture());

        List<String> cipherler = cap.getAllValues().stream().map(SifreKasa::getSifreCipher).toList();
        assertNotEquals(cipherler.get(0), cipherler.get(1));
    }

    @Test
    void hataliAnahtarCozemez() {
        String cipher = ReflectionTestUtils.invokeMethod(servis, "sifrele", "Gizli123!");
        ReflectionTestUtils.setField(servis, "encryptionKey", "baska-bir-anahtar-degeri-xx");

        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(servis, "coz", cipher));
    }

    @Test
    void anahtarYoksaSifrelemeReddedilir() {
        // AiConfigService gibi sessiz gecici anahtar URETILMEZ: restart
        // sonrasi tum sifreler okunmaz olurdu.
        ReflectionTestUtils.setField(servis, "encryptionKey", "");
        BusinessException hata = assertThrows(BusinessException.class,
                () -> ReflectionTestUtils.invokeMethod(servis, "sifrele", "x"));
        assertTrue(hata.getMessage().contains("APP_VAULT_ENCRYPTION_KEY"));
    }

    @Test
    void anahtarYoksaOkumaDaReddedilir() {
        ReflectionTestUtils.setField(servis, "encryptionKey", "");
        when(repo.findById(1L)).thenReturn(Optional.of(kayit(1L, "KISISEL", "SAHIS", KULLANICI)));
        assertThrows(BusinessException.class, () -> servis.sifreyiGoster(1L, SIRKET, KULLANICI, false, "1.2.3.4"));
    }

    // ============================================================
    // 2) LISTEDE SIFRE CIKMAZ
    // ============================================================

    @Test
    void listele_ciktisindaSifreAlaniYoktur() {
        when(repo.kisiselKayitlar(eq(SIRKET), eq(KULLANICI), isNull()))
                .thenReturn(List.of(kayit(1L, "KISISEL", "SAHIS", KULLANICI)));

        List<SifreKasaDTO> liste = servis.listele(SIRKET, KULLANICI, "KISISEL", null, null, null, false);

        assertEquals(1, liste.size());
        assertNull(liste.get(0).getSifre(), "liste ciktisinda sifre OLMAMALI");
    }

    @Test
    void idyeGoreGetir_sifreDondurmez() {
        when(repo.findById(1L)).thenReturn(Optional.of(kayit(1L, "GLOBAL", "SAHIS", null)));
        assertNull(servis.idyeGoreGetir(1L, SIRKET, KULLANICI).getSifre());
    }

    // ============================================================
    // 3) GORUNURLUK MATRISI
    // ============================================================

    @Test
    void kisiselKaydinSifresiniSahibiAcar() {
        when(repo.findById(1L)).thenReturn(Optional.of(kayit(1L, "KISISEL", "SAHIS", KULLANICI)));
        // gercek cipher uret
        SifreKasa k = kayit(1L, "KISISEL", "SAHIS", KULLANICI);
        k.setSifreCipher(ReflectionTestUtils.invokeMethod(servis, "sifrele", "SahibinSifresi"));
        when(repo.findById(1L)).thenReturn(Optional.of(k));

        assertEquals("SahibinSifresi", servis.sifreyiGoster(1L, SIRKET, KULLANICI, false, "1.1.1.1"));
    }

    @Test
    void kisiselKaydinSifresiniBaskasiAcamaz() {
        SifreKasa k = kayit(1L, "KISISEL", "SAHIS", KULLANICI);
        k.setSifreCipher(ReflectionTestUtils.invokeMethod(servis, "sifrele", "SahibinSifresi"));
        when(repo.findById(1L)).thenReturn(Optional.of(k));

        // Varlik sizdirmamak icin 404 (ResourceNotFoundException)
        assertThrows(ResourceNotFoundException.class,
                () -> servis.sifreyiGoster(1L, SIRKET, 55L, false, "1.1.1.1"));
    }

    @Test
    void globalSahisKaydinSifresiniBaskasiAcamaz() {
        SifreKasa k = kayit(2L, "GLOBAL", "SAHIS", ADMIN);
        k.setSifreCipher(ReflectionTestUtils.invokeMethod(servis, "sifrele", "SirketSifresi"));
        when(repo.findById(2L)).thenReturn(Optional.of(k));

        assertThrows(ResourceNotFoundException.class,
                () -> servis.sifreyiGoster(2L, SIRKET, 55L, false, "1.1.1.1"));
    }

    @Test
    void globalSahisKaydinSifresiniAdminAcar() {
        SifreKasa k = kayit(2L, "GLOBAL", "SAHIS", ADMIN);
        k.setSifreCipher(ReflectionTestUtils.invokeMethod(servis, "sifrele", "SirketSifresi"));
        when(repo.findById(2L)).thenReturn(Optional.of(k));

        assertEquals("SirketSifresi", servis.sifreyiGoster(2L, SIRKET, 55L, true, "1.1.1.1"));
    }

    @Test
    void globalTumuKaydinSifresiniHerkesAcar() {
        SifreKasa k = kayit(3L, "GLOBAL", "TUMU", ADMIN);
        k.setSifreCipher(ReflectionTestUtils.invokeMethod(servis, "sifrele", "OfisSifresi"));
        when(repo.findById(3L)).thenReturn(Optional.of(k));

        // sifre_gorunurlugu=TUMU: normal USER da acabilir
        assertEquals("OfisSifresi", servis.sifreyiGoster(3L, SIRKET, 55L, false, "1.1.1.1"));
    }

    @Test
    void sifreAcmaDenetimIzeYazilir() {
        SifreKasa k = kayit(3L, "GLOBAL", "TUMU", ADMIN);
        k.setSifreCipher(ReflectionTestUtils.invokeMethod(servis, "sifrele", "OfisSifresi"));
        when(repo.findById(3L)).thenReturn(Optional.of(k));

        servis.sifreyiGoster(3L, SIRKET, 55L, false, "9.9.9.9");

        verify(auditLogService).log(eq(55L), eq(SIRKET), eq("SIFRE_GORUNTULENME"),
                eq("SifreKasa"), eq(3L), anyString(), eq("9.9.9.9"), anyString());
    }

    @Test
    void sifreAcmaSonGoruntulemeDamgasi() {
        SifreKasa k = kayit(3L, "GLOBAL", "TUMU", ADMIN);
        k.setSifreCipher(ReflectionTestUtils.invokeMethod(servis, "sifrele", "X"));
        when(repo.findById(3L)).thenReturn(Optional.of(k));

        servis.sifreyiGoster(3L, SIRKET, 55L, false, "1.1.1.1");

        assertNotNull(repo.findById(3L).orElseThrow().getSonGoruntuleme());
        verify(repo).save(any(SifreKasa.class));
    }

    // ============================================================
    // 4) YAZMA YETKISI
    // ============================================================

    @Test
    void normalKullaniciKisiselKayitYazabilir() {
        when(repo.existsBySirketIdAndBaslikIgnoreCaseAndAktifTrue(anyLong(), anyString())).thenReturn(false);
        kaydetVeDon(null);
        assertDoesNotThrow(() -> servis.olustur(form("Kisisel", "Sifre1"), SIRKET, KULLANICI, false));
    }

    @Test
    void normalKullaniciGlobalKayitYazamaz() {
        SifreKasaDTO dto = form("Global", "Sifre1");
        dto.setKapsam("GLOBAL");
        BusinessException hata = assertThrows(BusinessException.class,
                () -> servis.olustur(dto, SIRKET, KULLANICI, false));
        assertTrue(hata.getMessage().contains("yönetici"));
        verify(repo, never()).save(any());
    }

    @Test
    void adminGlobalKayitYazabilir() {
        when(repo.existsBySirketIdAndBaslikIgnoreCaseAndAktifTrue(anyLong(), anyString())).thenReturn(false);
        kaydetVeDon(null);
        SifreKasaDTO dto = form("Global", "Sifre1");
        dto.setKapsam("GLOBAL");
        assertDoesNotThrow(() -> servis.olustur(dto, SIRKET, ADMIN, true));
    }

    @Test
    void tumuGorunurlukYalnizGlobalKapsamda() {
        SifreKasaDTO dto = form("Sifreli", "Sifre1");
        dto.setKapsam("KISISEL");
        dto.setSifreGorunurlugu("TUMU");
        BusinessException hata = assertThrows(BusinessException.class,
                () -> servis.olustur(dto, SIRKET, KULLANICI, false));
        assertTrue(hata.getMessage().contains("global"));
    }

    @Test
    void gecersizKategoriReddedilir() {
        SifreKasaDTO dto = form("X", "Sifre1");
        dto.setKategori("UYGULAMADI");
        assertThrows(BusinessException.class, () -> servis.olustur(dto, SIRKET, KULLANICI, false));
    }

    @Test
    void negatifGecerlilikReddedilir() {
        SifreKasaDTO dto = form("X", "Sifre1");
        dto.setGecerlilikGun(-5);
        assertThrows(BusinessException.class, () -> servis.olustur(dto, SIRKET, KULLANICI, false));
    }

    @Test
    void bosSifreIleKayitReddedilir() {
        assertThrows(BusinessException.class,
                () -> servis.olustur(form("X", ""), SIRKET, KULLANICI, false));
    }

    @Test
    void ayniBaslikIkiKezEklenemez() {
        when(repo.existsBySirketIdAndBaslikIgnoreCaseAndAktifTrue(SIRKET, "Netsis")).thenReturn(true);
        BusinessException hata = assertThrows(BusinessException.class,
                () -> servis.olustur(form("Netsis", "S"), SIRKET, KULLANICI, false));
        assertTrue(hata.getMessage().contains("zaten var"));
    }

    // ============================================================
    // 5) GUNCELLEME / SURE
    // ============================================================

    @Test
    void guncellemeBosSifreMevcutSifreyiKorur() {
        SifreKasa k = kayit(1L, "KISISEL", "SAHIS", KULLANICI);
        k.setSifreCipher(ReflectionTestUtils.invokeMethod(servis, "sifrele", "EskiSifre"));
        when(repo.findById(1L)).thenReturn(Optional.of(k));
        kaydetVeDon(k);

        SifreKasaDTO dto = SifreKasaDTO.builder().baslik("Yeni Baslik").build(); // sifre YOK
        servis.guncelle(1L, dto, SIRKET, KULLANICI, false);

        assertEquals("EskiSifre",
                ReflectionTestUtils.invokeMethod(servis, "coz", k.getSifreCipher()));
        assertEquals("Yeni Baslik", k.getBaslik());
    }

    @Test
    void guncellemeSifreDegisirseSureSifirlanir() {
        SifreKasa k = kayit(1L, "KISISEL", "SAHIS", KULLANICI);
        k.setGecerlilikGun(90);
        k.setSifreDegisimTarihi(LocalDateTime.now().minusDays(50));
        when(repo.findById(1L)).thenReturn(Optional.of(k));
        kaydetVeDon(k);

        SifreKasaDTO dto = form("Ayni Baslik", "YeniSifre");
        servis.guncelle(1L, dto, SIRKET, KULLANICI, false);

        assertTrue(k.getSifreDegisimTarihi().isAfter(LocalDateTime.now().minusDays(1)));
        assertEquals("YeniSifre", ReflectionTestUtils.invokeMethod(servis, "coz", k.getSifreCipher()));
    }

    @Test
    void guncellemeSifreDegismeseSureUzatilmaz() {
        LocalDateTime eski = LocalDateTime.now().minusDays(50);
        SifreKasa k = kayit(1L, "KISISEL", "SAHIS", KULLANICI);
        k.setGecerlilikGun(90);
        k.setSifreDegisimTarihi(eski);
        when(repo.findById(1L)).thenReturn(Optional.of(k));
        kaydetVeDon(k);

        SifreKasaDTO dto = SifreKasaDTO.builder().baslik("Yeni Baslik").build();
        servis.guncelle(1L, dto, SIRKET, KULLANICI, false);

        assertEquals(eski, k.getSifreDegisimTarihi(),
                "yalniz baslik degisince sure referansi kaymamali");
    }

    @Test
    void baskasininKaydiGuncellenemez() {
        when(repo.findById(1L)).thenReturn(Optional.of(kayit(1L, "KISISEL", "SAHIS", KULLANICI)));
        assertThrows(ResourceNotFoundException.class,
                () -> servis.guncelle(1L, form("X", "Y"), SIRKET, 55L, false));
    }

    // --- sure durumu (4 senaryo) ---

    @Test
    void sureDurumu_suresiz() {
        SifreKasa k = kayit(1L, "KISISEL", "SAHIS", KULLANICI);
        assertEquals("SURESIZ", SifreKasaService.sureDurumu(k, LocalDateTime.now()));
    }

    @Test
    void sureDurumu_gecerli() {
        SifreKasa k = kayit(1L, "KISISEL", "SAHIS", KULLANICI);
        k.setGecerlilikGun(90);
        k.setSifreDegisimTarihi(LocalDateTime.now().minusDays(10));
        assertEquals("GECERLI", SifreKasaService.sureDurumu(k, LocalDateTime.now()));
    }

    @Test
    void sureDurumu_yaklasti() {
        SifreKasa k = kayit(1L, "KISISEL", "SAHIS", KULLANICI);
        k.setGecerlilikGun(90);
        k.setSifreDegisimTarihi(LocalDateTime.now().minusDays(80));
        assertEquals("SURE_YAKLASTI", SifreKasaService.sureDurumu(k, LocalDateTime.now()));
    }

    @Test
    void sureDurumu_bitti() {
        SifreKasa k = kayit(1L, "KISISEL", "SAHIS", KULLANICI);
        k.setGecerlilikGun(90);
        k.setSifreDegisimTarihi(LocalDateTime.now().minusDays(100));
        assertEquals("SURESI_BITTI", SifreKasaService.sureDurumu(k, LocalDateTime.now()));
    }

    @Test
    void sureDurumu_esikTam14Gun() {
        // 90 gunluk surenin tam 14 gunu kalmasinda uyari baslamali.
        SifreKasa k = kayit(1L, "KISISEL", "SAHIS", KULLANICI);
        k.setGecerlilikGun(90);
        k.setSifreDegisimTarihi(LocalDateTime.now().minusDays(76));
        assertEquals("SURE_YAKLASTI", SifreKasaService.sureDurumu(k, LocalDateTime.now()));
    }

    @Test
    void uyariEsigiSabit14Gun() {
        assertEquals(14, SifreKasaService.UYARI_ESIGI_GUN);
    }

    // ============================================================
    // 6) ARSIVLEME (soft delete)
    // ============================================================

    @Test
    void arsivlemeFizikselSilmez() {
        SifreKasa k = kayit(1L, "KISISEL", "SAHIS", KULLANICI);
        when(repo.findById(1L)).thenReturn(Optional.of(k));
        kaydetVeDon(k);

        servis.arsivle(1L, SIRKET, KULLANICI, false);

        verify(repo, never()).deleteById(anyLong());
        verify(repo, never()).delete(any());
        assertFalse(k.getAktif());
        assertNotNull(k.getArsivTarihi());
    }

    @Test
    void arsivdenGeriAlmaKaydiAktifler() {
        SifreKasa k = kayit(1L, "KISISEL", "SAHIS", KULLANICI);
        k.setAktif(false);
        k.setArsivTarihi(LocalDateTime.now());
        when(repo.findById(1L)).thenReturn(Optional.of(k));
        kaydetVeDon(k);

        servis.arsivleGeriAl(1L, SIRKET, KULLANICI, false);

        assertTrue(k.getAktif());
        assertNull(k.getArsivTarihi());
    }

    @Test
    void arsivlemeIdempotent() {
        SifreKasa k = kayit(1L, "KISISEL", "SAHIS", KULLANICI);
        k.setAktif(false);
        when(repo.findById(1L)).thenReturn(Optional.of(k));
        servis.arsivle(1L, SIRKET, KULLANICI, false);
        verify(repo, never()).save(any());
    }

    @Test
    void arsivlenenKayitListedenCikarilabilir() {
        when(repo.kisiselKayitlar(eq(SIRKET), eq(KULLANICI), eq(true))).thenReturn(List.of());
        assertTrue(servis.listele(SIRKET, KULLANICI, "KISISEL", true, null, null, false).isEmpty());
        verify(repo).kisiselKayitlar(SIRKET, KULLANICI, true);
    }

    @Test
    void ozetArsivSayisiniVerir() {
        when(repo.countBySirketIdAndAktifFalse(SIRKET)).thenReturn(3L);
        SifreKasaOzetDTO ozet = servis.ozet(SIRKET, KULLANICI);
        assertEquals(3L, ozet.getArsiv());
    }

    // ============================================================
    // 7) TENANT
    // ============================================================

    @Test
    void baskaSirketKaydiBulunamaz() {
        SifreKasa k = SifreKasa.builder().id(1L).sirketId(999L).baslik("X")
                .sifreCipher("c").aktif(true).build();
        when(repo.findById(1L)).thenReturn(Optional.of(k));
        doThrow(new ResourceNotFoundException("SifreKasa bu sirkete ait degil"))
                .when(tenantChecker).check(anyLong(), anyString());

        assertThrows(ResourceNotFoundException.class,
                () -> servis.idyeGoreGetir(1L, SIRKET, KULLANICI));
    }

    @Test
    void sirketsizIstekReddedilir() {
        assertThrows(BusinessException.class,
                () -> servis.olustur(form("X", "Y"), null, KULLANICI, false));
    }
}