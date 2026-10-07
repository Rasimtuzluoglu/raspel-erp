package com.raspel.erp.service.sistem;

import com.raspel.erp.config.security.JwtUtil;
import com.raspel.erp.dto.sistem.KullaniciDTO;
import com.raspel.erp.dto.sistem.LoginRequest;
import com.raspel.erp.dto.sistem.LoginResponse;
import com.raspel.erp.dto.sistem.TwoFactorGirisRequest;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import com.raspel.erp.util.TotpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import com.raspel.erp.exception.BusinessException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KullaniciServiceTest {

    @Mock
    private KullaniciRepository kullaniciRepository;
    @Mock
    private SirketRepository sirketRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private AktifOturumService aktifOturumService;
    @Mock
    private com.raspel.erp.config.TenantChecker tenantChecker;
    @Mock
    private com.raspel.erp.repository.sistem.SifreSifirlaTokenRepository sifreSifirlaTokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private com.raspel.erp.service.sistem.AuditLogService auditLogService;
    // KullaniciService giris denemelerini sayar (raspel.giris.deneme). Provider
    // null donerse metrik atlanir; testte bos provider yeterlidir.
    @Mock
    private org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterRegistryProvider;

    @InjectMocks
    private KullaniciService kullaniciService;

    private Kullanici createKullanici(Long id) {
        Kullanici k = new Kullanici();
        k.setId(id);
        k.setUsername("testuser" + id);
        k.setPassword("encoded");
        k.setDisplayName("Test User");
        k.setRole("USER");
        k.setActive(true);
        k.setOlusturmaTarihi(LocalDateTime.now());
        return k;
    }

@Test
    void tumunuGetir_nullSirketBosDoner() {
        Page<KullaniciDTO> result = kullaniciService.tumunuGetir(null, Pageable.unpaged());
        assertTrue(result.isEmpty());
        verify(kullaniciRepository, never()).findAll(any(Pageable.class));
    }

    // ------------------------------------------------------------------
    // REDTEAM H-1/H-2 regresyonu: platform geneli erisim kontrolu
    //
    // CANLI KANIT: "ZZTEST Sirket B"nin ADMIN'i (sirket 99) GET /api/backups ile
    // TUM sirketlerin (RasPel Test dahil) veritabani yedeklerini listeledi ve
    // indirebildi; ayrica klasor=backups ile presigned URL aldi. Yedek tek DB
    // dump'u oldugu icin icinde TUM tenant'larin verisi vardir. Bu yuzden bu
    // islemler sirket basina ADMIN degil, PLATFORM YONETICISI seviyesinde
    // korunmalidir.
    // ------------------------------------------------------------------

    private void kimlikBagla(Kullanici k) {
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                k.getUsername(), null,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_" + k.getRole())));
        org.springframework.security.core.context.SecurityContextHolder
                .getContext().setAuthentication(auth);
    }

    @AfterEach
    void securityContextTemizle() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("H-1: beyaz listedeki ADMIN platform yoneticisidir")
    void platformYoneticisiMi_beyazListedekiAdminTrue() {
        Kullanici admin = createKullanici(1L);
        admin.setUsername("admin");
        admin.setRole("ADMIN");
        when(kullaniciRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        kimlikBagla(admin);

        assertTrue(kullaniciService.platformYoneticisiMi());
        kullaniciService.platformYoneticisiGerekir("test");
    }

    @Test
    @DisplayName("H-1: ADMIN olsa bile beyaz listede yoksa erisim reddedilir")
    void platformYoneticisiMi_beyazListedeOlmayanAdminReddedilir() {
        // CANLI KANIT: "ZZTEST Sirket B"nin ADMIN'i (zzadmin_b) beyaz listede
        // olmadigi icin artik platform islemlerine erisemez.
        Kullanici k = createKullanici(9901L);
        k.setUsername("zzadmin_b");
        k.setRole("ADMIN");
        // Beyaz liste kontrolunden elenir; DB'ye gidilmez.
        lenient().when(kullaniciRepository.findByUsername("zzadmin_b")).thenReturn(Optional.of(k));
        kimlikBagla(k);

        assertFalse(kullaniciService.platformYoneticisiMi(),
                "Sirket admin'i platform yoneticisi sayilmamali (canli kanit: yedek sizintisi)");
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> kullaniciService.platformYoneticisiGerekir("yedek listeleme"));
    }

    @Test
    @DisplayName("H-1: USER/DRIVER/SAHA platform yoneticisi DEGILDIR")
    void platformYoneticisiMi_adminOlmayanReddedilir() {
        for (String rol : List.of("USER", "DRIVER", "SAHA", "MUHASEBE")) {
            Kullanici k = createKullanici(2L);
            k.setRole(rol);
            // ADMIN yetkisi olmadigi icin kod DB'ye hic gitmeden reddeder;
            // stub yine de hazir bekletilir (asiri kesinlik birakmamak icin).
            lenient().when(kullaniciRepository.findByUsername(k.getUsername())).thenReturn(Optional.of(k));
            kimlikBagla(k);

            assertFalse(kullaniciService.platformYoneticisiMi(),
                    rol + " rolundeki kullanici platform yoneticisi sayilmamali");
            assertThrows(com.raspel.erp.exception.BusinessException.class,
                    () -> kullaniciService.platformYoneticisiGerekir("yedek listeleme"));
        }
    }

    @Test
    @DisplayName("H-1: pasif (active=false) beyaz listeli ADMIN reddedilir")
    void platformYoneticisiMi_pasifAdminReddedilir() {
        Kullanici admin = createKullanici(1L);
        admin.setUsername("admin");
        admin.setRole("ADMIN");
        admin.setActive(false);
        when(kullaniciRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        kimlikBagla(admin);

        assertFalse(kullaniciService.platformYoneticisiMi(),
                "Devre disi birakilmis hesap platform islemi yapmamali");
    }

    @Test
    @DisplayName("H-1: oturum yoksa erisim reddedilir (fail-closed)")
    void platformYoneticisiMi_oturumYoksaReddedilir() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        assertFalse(kullaniciService.platformYoneticisiMi());
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> kullaniciService.platformYoneticisiGerekir("yedek indirme"));
    }

    @Test
    @DisplayName("H-1: DB'de olmayan kullanici fail-closed reddedilir")
    void platformYoneticisiMi_kullaniciBilinmiyorReddedilir() {
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "admin", null,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        when(kullaniciRepository.findByUsername("admin")).thenReturn(Optional.empty());

        assertFalse(kullaniciService.platformYoneticisiMi());
    }

    @Test
    void tumunuGetir_returnsTenantUsers() {
        when(kullaniciRepository.findBySirketId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createKullanici(1L), createKullanici(2L))));
        Page<KullaniciDTO> result = kullaniciService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void getir_returnsUser() {
        Kullanici k = createKullanici(1L);
        k.setSirketId(1L);
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));
        when(tenantChecker.getCurrentKullaniciId()).thenReturn(1L);
        KullaniciDTO result = kullaniciService.getir(1L);
        assertEquals("testuser1", result.getUsername());
    }

    @Test
    void getir_kendiKaydi_sirketDegisseBileDoner() {
        // Kullanicinin kayitli (home) sirketi 1; JWT aktif sirketi 2 (sirket degistirdi).
        // /ben cagrisi self-lock olmamali.
        Kullanici k = createKullanici(1L);
        k.setSirketId(1L);
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));
        when(tenantChecker.getCurrentKullaniciId()).thenReturn(1L);

        KullaniciDTO result = kullaniciService.getir(1L);

        assertEquals("testuser1", result.getUsername());
        verify(tenantChecker, never()).check(any(), any());
    }

    @Test
    void getir_baskasininKaydi_farkliSirketteReddedilir() {
        // Baska bir kullanicinin kaydini, o kullanicinin home sirketi aktif sirketle
        // eslesmiyorsa ve atanmis sirket yoksa okuyamaz.
        Kullanici k = createKullanici(5L);
        k.setSirketId(3L);
        k.setSirketler(java.util.Set.of());
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(k));
        when(tenantChecker.getCurrentKullaniciId()).thenReturn(1L);
        when(tenantChecker.getCurrentSirketId()).thenReturn(2L);

        assertThrows(com.raspel.erp.exception.ResourceNotFoundException.class,
                () -> kullaniciService.getir(5L));
    }

    @Test
    void getir_baskasininKaydi_atanmisSirketteDoner() {
        Kullanici k = createKullanici(5L);
        k.setSirketId(3L);
        com.raspel.erp.entity.sistem.Sirket s = new com.raspel.erp.entity.sistem.Sirket();
        s.setId(2L);
        k.setSirketler(java.util.Set.of(s));
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(k));
        when(tenantChecker.getCurrentKullaniciId()).thenReturn(1L);
        when(tenantChecker.getCurrentSirketId()).thenReturn(2L);

        KullaniciDTO result = kullaniciService.getir(5L);

        assertEquals("testuser5", result.getUsername());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> kullaniciService.getir(99L));
    }

    @Test
    void olustur_createsUser() {
        KullaniciDTO dto = KullaniciDTO.builder().username("newuser").displayName("New").password("Password123!").build();
        when(kullaniciRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded");
        Kullanici saved = createKullanici(1L);
        saved.setUsername("newuser");
        saved.setDisplayName("New");
        when(kullaniciRepository.save(any(Kullanici.class))).thenReturn(saved);
        KullaniciDTO result = kullaniciService.olustur(dto);
        assertEquals("newuser", result.getUsername());
    }

    @Test
    void olustur_throwsWhenUsernameExists() {
        KullaniciDTO dto = KullaniciDTO.builder().username("existing").displayName("Existing").build();
        assertThrows(RuntimeException.class, () -> kullaniciService.olustur(dto));
    }

    @Test
    void guncelle_updatesUser() {
        Kullanici existing = createKullanici(1L);
        existing.setSirketId(1L);
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tenantChecker.getCurrentSirketId()).thenReturn(1L);
        KullaniciDTO dto = KullaniciDTO.builder().displayName("Updated").active(false).role("ADMIN").build();
        when(kullaniciRepository.save(any(Kullanici.class))).thenReturn(existing);
        KullaniciDTO result = kullaniciService.guncelle(1L, dto);
        assertEquals("Updated", result.getDisplayName());
    }

    @Test
    void guncelle_baskaSirketteReddedilir() {
        Kullanici existing = createKullanici(5L);
        existing.setSirketId(3L);
        existing.setSirketler(java.util.Set.of());
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(tenantChecker.getCurrentSirketId()).thenReturn(2L);

        assertThrows(com.raspel.erp.exception.ResourceNotFoundException.class,
                () -> kullaniciService.guncelle(5L, KullaniciDTO.builder().displayName("X").build()));
    }

@Test
    void guncelle_throwsWhenNotFound() {
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> kullaniciService.guncelle(99L, new KullaniciDTO()));
    }

    // ------------------------------------------------------------------
    // REDTEAM C9 regresyonu: sifre degisimi token iptal etmiyordu.
    //
    // CANLI KANIT: PUT /api/kullanicilar/9905 ile sifre degistirildi -> HTTP 200,
    // ancak token_version 0'da kaldi ve SIFRE DEGISIMINDEN ONCE alinan eski token
    // hala HTTP 200 donuyordu. Sifre degistirilince tum oturumlarin aninda
    // sonlanmasi gerekir.
    // ------------------------------------------------------------------

    @Test
    @DisplayName("C9: sifre degisimi tokenVersion'i artirir (eski token'lar iptal edilir)")
    void guncelle_sifreDegisimiTokenVersionArtirir() {
        Kullanici existing = createKullanici(1L);
        existing.setSirketId(1L);
        existing.setTokenVersion(0L);
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tenantChecker.getCurrentSirketId()).thenReturn(1L);
        when(kullaniciRepository.save(any(Kullanici.class))).thenAnswer(inv -> inv.getArgument(0));

        KullaniciDTO dto = KullaniciDTO.builder()
                .password("YeniCokGucluSifre123!")
                .build();
        kullaniciService.guncelle(1L, dto);

        assertEquals(Long.valueOf(1L), existing.getTokenVersion(),
                "Sifre degisimi tokenVersion artirmaliydi (canli kanit: 0'da kalmisti)");
    }

    @Test
    @DisplayName("C9: sirket uyeligi degisimi de tokenVersion'i artirir")
    void guncelle_sirketUyeligiDegisimiTokenVersionArtirir() {
        Kullanici existing = createKullanici(1L);
        existing.setSirketId(1L);
        existing.setSirketler(new java.util.HashSet<>(java.util.Set.of(
                com.raspel.erp.entity.sistem.Sirket.builder().id(1L).build())));
        existing.setTokenVersion(0L);
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tenantChecker.getCurrentSirketId()).thenReturn(1L);
        when(sirketRepository.findById(anyLong())).thenReturn(Optional.of(
                com.raspel.erp.entity.sistem.Sirket.builder().id(2L).build()));
        when(kullaniciRepository.save(any(Kullanici.class))).thenAnswer(inv -> inv.getArgument(0));

        KullaniciDTO dto = KullaniciDTO.builder()
                .sirketIds(List.of(1L, 2L))
                .build();
        kullaniciService.guncelle(1L, dto);

        assertEquals(Long.valueOf(1L), existing.getTokenVersion(),
                "Sirket uyeligi degisimi tokenVersion artirmaliydi");
    }

    @Test
    @DisplayName("C9: sifre degismiyorsa tokenVersion artmaz (kullaniciyi gereksiz kilitlemez)")
    void guncelle_sifreDegismiyorsaTokenVersionArtmaz() {
        Kullanici existing = createKullanici(1L);
        existing.setSirketId(1L);
        existing.setTokenVersion(0L);
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tenantChecker.getCurrentSirketId()).thenReturn(1L);
        when(kullaniciRepository.save(any(Kullanici.class))).thenAnswer(inv -> inv.getArgument(0));

        KullaniciDTO dto = KullaniciDTO.builder().displayName("Yeni Gorunen Ad").build();
        kullaniciService.guncelle(1L, dto);

        assertEquals(Long.valueOf(0L), existing.getTokenVersion(),
                "Profil guncellemesi mevcut oturumlari kilitlememeli");
    }

    @Test
    void sil_deletesUser() {
        Kullanici k = createKullanici(1L);
        k.setSirketId(1L);
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));
        when(tenantChecker.getCurrentSirketId()).thenReturn(1L);
        kullaniciService.sil(1L);
        verify(kullaniciRepository).delete(any(Kullanici.class));
    }

    @Test
    void sil_baskaSirketteReddedilir() {
        Kullanici k = createKullanici(5L);
        k.setSirketId(3L);
        k.setSirketler(java.util.Set.of());
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(k));
        when(tenantChecker.getCurrentSirketId()).thenReturn(2L);

        assertThrows(com.raspel.erp.exception.ResourceNotFoundException.class,
                () -> kullaniciService.sil(5L));
        verify(kullaniciRepository, never()).delete(any(Kullanici.class));
    }

    @Test
    void sil_throwsWhenNotFound() {
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> kullaniciService.sil(99L));
    }

    @Test
    void giris_successfulLogin() {
        Kullanici k = createKullanici(1L);
        k.setPassword("encoded");
        when(kullaniciRepository.findByUsername("testuser1")).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser1");
        req.setPassword("pass");
        LoginResponse resp = kullaniciService.giris(req);
        assertEquals("testuser1", resp.getUsername());
        assertNotNull(resp.getGirisToken());
        assertFalse(Boolean.TRUE.equals(resp.getTwoFactorGerekli()));
    }

    @Test
    void giris_throwsWhenInvalidPassword() {
        Kullanici k = createKullanici(1L);
        k.setPassword("encoded");
        when(kullaniciRepository.findByUsername("testuser1")).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser1");
        req.setPassword("wrong");
        assertThrows(RuntimeException.class, () -> kullaniciService.giris(req));
    }

    @Test
    void giris_throwsWhenInactive() {
        Kullanici k = createKullanici(1L);
        k.setActive(false);
        k.setPassword("encoded");
        when(kullaniciRepository.findByUsername("testuser1")).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser1");
        req.setPassword("pass");
        assertThrows(RuntimeException.class, () -> kullaniciService.giris(req));
    }

    // ---------- B2h: kullanici numaralandirmasi (enumeration) ve zamanlama ----------

    /**
     * Kullanıcı adı varlığını sızdırmaz: kullanıcı yok / pasif / yanlış şifre
     * durumlarının ÜÇÜ de aynı mesajı döner.
     */
    @Test
    void giris_yoksaPasifseVeHataliSifredeAyniMesajiDoner() {
        // 1) Kullanıcı yok
        when(kullaniciRepository.findByUsername("yok")).thenReturn(Optional.empty());
        var hataYok = assertThrows(BusinessException.class, () -> kullaniciService.giris(
                LoginRequest.builder().username("yok").password("p").build()));

        // 2) Var ama pasif (şifre DOĞRU olsa bile)
        Kullanici pasif = createKullanici(2L);
        pasif.setActive(false);
        pasif.setPassword("encoded");
        when(kullaniciRepository.findByUsername("pasif")).thenReturn(Optional.of(pasif));
        when(passwordEncoder.matches("dogru", "encoded")).thenReturn(true);
        var hataPasif = assertThrows(BusinessException.class, () -> kullaniciService.giris(
                LoginRequest.builder().username("pasif").password("dogru").build()));

        // 3) Var, aktif, şifre yanlış
        Kullanici aktif = createKullanici(3L);
        aktif.setPassword("encoded");
        when(kullaniciRepository.findByUsername("aktif")).thenReturn(Optional.of(aktif));
        when(passwordEncoder.matches("yanlis", "encoded")).thenReturn(false);
        var hataSifre = assertThrows(BusinessException.class, () -> kullaniciService.giris(
                LoginRequest.builder().username("aktif").password("yanlis").build()));

        assertEquals(hataYok.getMessage(), hataPasif.getMessage(),
                "Pasif kullanıcı mesajı diğerlerinden ayırt edilememeli");
        assertEquals(hataYok.getMessage(), hataSifre.getMessage(),
                "Hatalı şifre mesajı diğerlerinden ayırt edilememeli");
        assertFalse(hataPasif.getMessage().toLowerCase().contains("aktif"),
                "Hata mesajı 'aktif' bilgisini sızdırmamalı");
    }

    /**
     * Zamanlama eşitlemesi: kullanıcı bulunamadığında da BCrypt karşılaştırması
     * çağrılır. Aksi halde "kullanıcı yok" isteği ~1 ms, gerçek şifre kontrolü
     * ~100 ms sürer ve fark kullanıcı adlarını numaralandırmaya yeter.
     */
    @Test
    void giris_kullaniciBulunamayincaBcryptDogrulamaYapar() {
        when(kullaniciRepository.findByUsername("yok")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> kullaniciService.giris(
                LoginRequest.builder().username("yok").password("p").build()));

        verify(passwordEncoder).matches(eq("p"), anyString());
    }

    /** Kullanıcı yoksa deneme yapılmasa bile BCrypt çağrılır (null şifre dâhil). */
    @Test
    void giris_kullaniciBulunamayincaNullSifreDahilGuvenli() {
        when(kullaniciRepository.findByUsername("yok")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> kullaniciService.giris(
                LoginRequest.builder().username("yok").password(null).build()));

        verify(passwordEncoder).matches(eq(""), anyString());
    }

    @Test
    void giris_twoFactorAktifIseJwtUretmez() {
        Kullanici k = createKullanici(1L);
        k.setPassword("encoded");
        k.setTwoFactorEnabled(true);
        k.setTwoFactorSecret("JBSWY3DPEHPK3PXP");
        when(kullaniciRepository.findByUsername("testuser1")).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setUsername("testuser1");
        req.setPassword("pass");

        LoginResponse resp = kullaniciService.giris(req);

        assertTrue(Boolean.TRUE.equals(resp.getTwoFactorGerekli()));
        assertNotNull(resp.getGirisToken());
        assertNull(resp.getToken());
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void giris2faTamamla_dogruKodJwtDoner() {
        Kullanici k = createKullanici(1L);
        k.setUsername("testuser1");
        k.setPassword("encoded");
        k.setTwoFactorEnabled(true);
        k.setTwoFactorSecret("JBSWY3DPEHPK3PXP");

        when(kullaniciRepository.findByUsername("testuser1")).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
        LoginResponse pending = kullaniciService.giris(LoginRequest.builder()
                .username("testuser1").password("pass").build());

        String dogruKod = TotpUtil.generateCode("JBSWY3DPEHPK3PXP", System.currentTimeMillis());

        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));
        // Faz 0.11: atomik replay kontrolü ilk kullanımda 1 satır günceller.
        when(kullaniciRepository.totpCounterGuncelle(eq(1L), anyLong())).thenReturn(1);

        LoginResponse resp = kullaniciService.giris2faTamamla(
                TwoFactorGirisRequest.builder()
                        .girisToken(pending.getGirisToken()).code(dogruKod).build());

        assertNotNull(resp.getGirisToken());
        assertFalse(Boolean.TRUE.equals(resp.getTwoFactorGerekli()));
        assertNotNull(resp.getSirketler());
    }

    @Test
    void giris2faTamamla_yanlisKodReddedilir() {
        Kullanici k = createKullanici(1L);
        k.setUsername("testuser1");
        k.setPassword("encoded");
        k.setTwoFactorEnabled(true);
        k.setTwoFactorSecret("JBSWY3DPEHPK3PXP");

        when(kullaniciRepository.findByUsername("testuser1")).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
        LoginResponse pending = kullaniciService.giris(LoginRequest.builder()
                .username("testuser1").password("pass").build());

        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));

        assertThrows(RuntimeException.class, () -> kullaniciService.giris2faTamamla(
                TwoFactorGirisRequest.builder()
                        .girisToken(pending.getGirisToken()).code("000000").build()));
    }

    @Test
    void girisSirket_ikiFaktorDogrulanmadanJwtUretemez() {
        Kullanici k = createKullanici(1L);
        k.setPassword("encoded");
        k.setSirketId(5L);
        k.setTwoFactorEnabled(true);
        k.setTwoFactorSecret("JBSWY3DPEHPK3PXP");
        when(kullaniciRepository.findByUsername("testuser1")).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));

        LoginResponse pending = kullaniciService.giris(LoginRequest.builder()
                .username("testuser1").password("pass").build());

        // Sifre adimindan gelen token, 2FA dogrulanmadan JWT'ye cevrilemez (bypass kapali).
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> kullaniciService.girisSirket(pending.getGirisToken(), 5L));
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void girisSirket_ikiFaktorDogrulandiktanSonraGecer() {
        Kullanici k = createKullanici(1L);
        k.setPassword("encoded");
        k.setSirketId(5L);
        k.setTwoFactorEnabled(true);
        k.setTwoFactorSecret("JBSWY3DPEHPK3PXP");
        when(kullaniciRepository.findByUsername("testuser1")).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("jwt-token");
        when(kullaniciRepository.totpCounterGuncelle(eq(1L), anyLong())).thenReturn(1);

        LoginResponse pending = kullaniciService.giris(LoginRequest.builder()
                .username("testuser1").password("pass").build());
        String kod = TotpUtil.generateCode("JBSWY3DPEHPK3PXP", System.currentTimeMillis());
        LoginResponse dogrulanmis = kullaniciService.giris2faTamamla(TwoFactorGirisRequest.builder()
                .girisToken(pending.getGirisToken()).code(kod).build());

        LoginResponse sonuc = kullaniciService.girisSirket(dogrulanmis.getGirisToken(), 5L);

        assertEquals("jwt-token", sonuc.getToken());
        verify(jwtUtil).generateToken(any(), any(), any());
    }

    @Test
    void enableTwoFactor_gecersizKodReddedilir() {
        Kullanici k = createKullanici(1L);
        k.setTwoFactorSecret("JBSWY3DPEHPK3PXP");
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));

        assertThrows(RuntimeException.class, () -> kullaniciService.enableTwoFactor(1L, "000000"));
        assertFalse(Boolean.TRUE.equals(k.getTwoFactorEnabled()));
    }

    @Test
    void profilGuncelle_rolDegistiremez() {
        Kullanici k = createKullanici(1L);
        k.setRole("USER");
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));
        when(kullaniciRepository.save(any(Kullanici.class))).thenReturn(k);

        KullaniciDTO dto = KullaniciDTO.builder().displayName("Yeni Ad").role("ADMIN").build();
        KullaniciDTO result = kullaniciService.profilGuncelle(1L, dto);

        assertEquals("Yeni Ad", result.getDisplayName());
        assertEquals("USER", result.getRole());
    }

    @Test
    void bildirimTercihleriGetir_virgulluStringiListeyeCevirir() {
        Kullanici k = Kullanici.builder().id(1L).username("admin").role("ADMIN")
                .bildirimTercihleri("FATURA,HATA").build();
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));

        List<String> sonuc = kullaniciService.bildirimTercihleriGetir(1L);

        assertEquals(2, sonuc.size());
        assertEquals("FATURA", sonuc.get(0));
        assertEquals("HATA", sonuc.get(1));
    }

    @Test
    void bildirimTercihleriGetir_bosIseBosListeDoner() {
        Kullanici k = Kullanici.builder().id(1L).username("admin").role("ADMIN").build();
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));

        List<String> sonuc = kullaniciService.bildirimTercihleriGetir(1L);

        assertNotNull(sonuc);
        assertTrue(sonuc.isEmpty());
    }

    @Test
    void bildirimTercihleriGuncelle_listeyiKaydeder() {
        Kullanici k = Kullanici.builder().id(1L).username("admin").role("ADMIN").build();
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));

        List<String> sonuc = kullaniciService.bildirimTercihleriGuncelle(1L, List.of("FATURA", "ANOMALI"));

        assertEquals(2, sonuc.size());
        assertEquals("FATURA,ANOMALI", k.getBildirimTercihleri());
        verify(kullaniciRepository).save(k);
    }

    @Test
    void sifreDogrula_dogruSifreGecer() {
        Kullanici k = createKullanici(1L);
        k.setPassword("encoded");
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("dogru", "encoded")).thenReturn(true);

        assertDoesNotThrow(() -> kullaniciService.sifreDogrula(1L, "dogru"));
    }

    @Test
    void sifreDogrula_yanlisSifreReddedilir() {
        Kullanici k = createKullanici(1L);
        k.setPassword("encoded");
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("yanlis", "encoded")).thenReturn(false);

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> kullaniciService.sifreDogrula(1L, "yanlis"));
    }

    @Test
    void sifreDogrula_bosSifreReddedilir() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> kullaniciService.sifreDogrula(1L, "  "));
    }

    @Test
    void sifreSifirlamaTalebi_emailYoksaSessizDoner() {
        Kullanici k = createKullanici(1L);
        when(kullaniciRepository.findByUsername("testuser1")).thenReturn(Optional.of(k));

        kullaniciService.sifreSifirlamaTalebi("testuser1");

        verify(emailService, never()).htmlGonder(any(), any(), any());
        verify(sifreSifirlaTokenRepository, never()).save(any());
    }

    @Test
    void sifreSifirlamaTalebi_emailVarsaTokenKaydederVeGonderir() {
        Kullanici k = createKullanici(1L);
        k.setEmail("kullanici@example.com");
        when(kullaniciRepository.findByUsername("testuser1")).thenReturn(Optional.of(k));
        when(emailService.htmlGonder(eq("kullanici@example.com"), any(), any())).thenReturn(true);

        kullaniciService.sifreSifirlamaTalebi("testuser1");

        verify(sifreSifirlaTokenRepository).kullaniciTokenlariniGecersizKil(1L);
        verify(sifreSifirlaTokenRepository).save(any());
        verify(emailService).htmlGonder(eq("kullanici@example.com"), any(), any());
    }

    @Test
    void sifreSifirlamaTalebi_kullaniciYoksaSessizDoner() {
        when(kullaniciRepository.findByUsername("yok")).thenReturn(Optional.empty());

        kullaniciService.sifreSifirlamaTalebi("yok");

        verify(sifreSifirlaTokenRepository, never()).save(any());
        verify(emailService, never()).htmlGonder(any(), any(), any());
    }

    @Test
    void sifreSifirlamaOnayla_gecersizTokenReddedilir() {
        when(sifreSifirlaTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> kullaniciService.sifreSifirlamaOnayla("gecersiz", "YeniSifre123!"));
    }

    @Test
    void sifreSifirlamaOnayla_suresiDolmusTokenReddedilir() {
        var token = com.raspel.erp.entity.sistem.SifreSifirlaToken.builder()
                .id(5L).kullaniciId(1L).tokenHash("h")
                .sonKullanma(java.time.LocalDateTime.now().minusHours(2))
                .kullanildi(false).build();
        when(sifreSifirlaTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> kullaniciService.sifreSifirlamaOnayla("abc", "YeniSifre123!"));
    }

    @Test
    void sifreSifirlamaOnayla_gecerliTokenSifreyiGunceller() {
        Kullanici k = createKullanici(1L);
        k.setPassword("eski");
        var token = com.raspel.erp.entity.sistem.SifreSifirlaToken.builder()
                .id(5L).kullaniciId(1L).tokenHash("h")
                .sonKullanma(java.time.LocalDateTime.now().plusHours(1))
                .kullanildi(false).build();
        when(sifreSifirlaTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(sifreSifirlaTokenRepository.tokenKullanildiIsaretle(5L)).thenReturn(1);
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));
        when(passwordEncoder.encode("YeniSifre123!")).thenReturn("yeni-hash");

        kullaniciService.sifreSifirlamaOnayla("abc", "YeniSifre123!");

        assertEquals("yeni-hash", k.getPassword());
        assertEquals(1L, k.getTokenVersion());
        verify(sifreSifirlaTokenRepository).tokenKullanildiIsaretle(5L);
    }

    @Test
    void sifreSifirlamaOnayla_zatenKullanilmisTokenReddedilir() {
        // Faz 0.12: atomik işaretleme 0 satır güncellerse (başka istek kullandıysa) reddedilir.
        var token = com.raspel.erp.entity.sistem.SifreSifirlaToken.builder()
                .id(5L).kullaniciId(1L).tokenHash("h")
                .sonKullanma(java.time.LocalDateTime.now().plusHours(1))
                .kullanildi(false).build();
        when(sifreSifirlaTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(sifreSifirlaTokenRepository.tokenKullanildiIsaretle(5L)).thenReturn(0);

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> kullaniciService.sifreSifirlamaOnayla("abc", "YeniSifre123!"));
        verify(kullaniciRepository, never()).save(any());
    }

    @Test
    void oturumUzat_30DakikaEklerVeEskiTokeniIptalEder() {
        Kullanici k = createKullanici(1L);
        k.setSirketId(1L);
        com.raspel.erp.entity.sistem.Sirket sirket = new com.raspel.erp.entity.sistem.Sirket();
        sirket.setId(1L);
        sirket.setAd("Firma");
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));
        when(jwtUtil.getExpirationFromToken("eski")).thenReturn(System.currentTimeMillis() + 5 * 60 * 1000);
        // Aktif sirket token'dan korunur (home sirketine dusurulmez).
        when(jwtUtil.getSirketIdFromToken("eski")).thenReturn(1L);
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(sirket));
        when(jwtUtil.generateToken(eq(k), eq(1L), eq("Firma"), anyLong())).thenReturn("yeni");
        when(jwtUtil.getJtiFromToken("yeni")).thenReturn("jti-yeni");
        when(jwtUtil.getJtiFromToken("eski")).thenReturn("jti-eski");

        LoginResponse r = kullaniciService.oturumUzat(1L, "eski");

        assertNotNull(r);
        assertEquals("yeni", r.getToken());
        assertEquals(1L, r.getSirketId());
        // Bitis: mevcut bitis + 30 dk (veya simdi + 30 dk) civarinda olmali.
        assertTrue(r.getTokenExpiresAt() > System.currentTimeMillis() + 25 * 60 * 1000);
        verify(aktifOturumService).oturumKaydet(eq("jti-yeni"), eq(1L), any(), any(), any(), any());
        verify(aktifOturumService).oturumIptal("jti-eski");
    }

    @Test
    void oturumUzat_pasifKullaniciReddedilir() {
        Kullanici k = createKullanici(2L);
        k.setActive(false);
        when(kullaniciRepository.findById(2L)).thenReturn(Optional.of(k));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> kullaniciService.oturumUzat(2L, "eski"));
    }

    // Kaba kuvvet tespiti icin giriş denemesi sayaci uretilmelidir. Kullanici adi
    // etiket olarak KULLANILMAZ (PII + kardinalite); yalnizca sonuc etiketi vardir.
    @Test
    void giris_sayacBasarisizDenemeleriEtiketler() {
        io.micrometer.core.instrument.MeterRegistry registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        when(meterRegistryProvider.getIfAvailable()).thenReturn(registry);
        Kullanici k = createKullanici(1L);
        k.setPassword("$2a$10$encoded");
        when(kullaniciRepository.findByUsername("admin")).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("wrong", "$2a$10$encoded")).thenReturn(false);

        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> kullaniciService.giris(
                com.raspel.erp.dto.sistem.LoginRequest.builder().username("admin").password("wrong").build()));

        double hatali = registry.counter("raspel.giris.deneme", "sonuc", "hatali_sifre").count();
        assertEquals(1.0, hatali);
        assertNull(registry.find("raspel.giris.deneme").tags("sonuc", "admin").counter(),
                "Kullanici adi etiket olarak kullanilmamali (PII/kardinalite)");
    }

    @Test
    void giris_sayacKullaniciBulunamadiVePasifKullanici() {
        io.micrometer.core.instrument.MeterRegistry registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        when(meterRegistryProvider.getIfAvailable()).thenReturn(registry);
        when(kullaniciRepository.findByUsername("yok")).thenReturn(Optional.empty());

        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> kullaniciService.giris(
                com.raspel.erp.dto.sistem.LoginRequest.builder().username("yok").password("x").build()));
        assertEquals(1.0, registry.counter("raspel.giris.deneme", "sonuc", "kullanici_yok").count());

Kullanici pasif = createKullanici(2L);
        pasif.setActive(false);
        pasif.setPassword("encoded");
        when(kullaniciRepository.findByUsername("pasif")).thenReturn(Optional.of(pasif));
        // Pasiflik kontrolü şifre doğrulamasINDAN sonra yapılır (numaralandırma).
        when(passwordEncoder.matches("x", "encoded")).thenReturn(true);
        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> kullaniciService.giris(
                com.raspel.erp.dto.sistem.LoginRequest.builder().username("pasif").password("x").build()));
        assertEquals(1.0, registry.counter("raspel.giris.deneme", "sonuc", "pasif").count());
    }

    @Test
    void giris_sayacBasariliGiris() {
        io.micrometer.core.instrument.MeterRegistry registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        when(meterRegistryProvider.getIfAvailable()).thenReturn(registry);
        Kullanici k = createKullanici(1L);
        k.setPassword("$2a$10$encoded");
        when(kullaniciRepository.findByUsername("admin")).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("dogru", "$2a$10$encoded")).thenReturn(true);

        kullaniciService.giris(com.raspel.erp.dto.sistem.LoginRequest.builder()
                .username("admin").password("dogru").build());

        assertEquals(1.0, registry.counter("raspel.giris.deneme", "sonuc", "basarili").count());
    }

    // Metrik opsiyoneldir: MeterRegistry bean'i yoksa giris akisi calismaya devam etmelidir.
    @Test
    void giris_registryYoksaHataVermez() {
        when(meterRegistryProvider.getIfAvailable()).thenReturn(null);
        Kullanici k = createKullanici(1L);
        k.setPassword("$2a$10$encoded");
        when(kullaniciRepository.findByUsername("admin")).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("dogru", "$2a$10$encoded")).thenReturn(true);

        assertNotNull(kullaniciService.giris(com.raspel.erp.dto.sistem.LoginRequest.builder()
                .username("admin").password("dogru").build()));
    }
}
