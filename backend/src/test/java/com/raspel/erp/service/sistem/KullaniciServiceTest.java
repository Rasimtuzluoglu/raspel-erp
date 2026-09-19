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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void tumunuGetir_returnsTenantUsers() {
        when(kullaniciRepository.findBySirketId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createKullanici(1L), createKullanici(2L))));
        Page<KullaniciDTO> result = kullaniciService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void getir_returnsUser() {
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(createKullanici(1L)));
        KullaniciDTO result = kullaniciService.getir(1L);
        assertEquals("testuser1", result.getUsername());
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
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(existing));
        KullaniciDTO dto = KullaniciDTO.builder().displayName("Updated").active(false).role("ADMIN").build();
        when(kullaniciRepository.save(any(Kullanici.class))).thenReturn(existing);
        KullaniciDTO result = kullaniciService.guncelle(1L, dto);
        assertEquals("Updated", result.getDisplayName());
    }

    @Test
    void guncelle_throwsWhenNotFound() {
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> kullaniciService.guncelle(99L, new KullaniciDTO()));
    }

    @Test
    void sil_deletesUser() {
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(createKullanici(1L)));
        kullaniciService.sil(1L);
        verify(kullaniciRepository).delete(any(Kullanici.class));
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
        when(kullaniciRepository.findByUsername("testuser1")).thenReturn(Optional.of(k));
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser1");
        req.setPassword("pass");
        assertThrows(RuntimeException.class, () -> kullaniciService.giris(req));
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
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));
        when(passwordEncoder.encode("YeniSifre123!")).thenReturn("yeni-hash");

        kullaniciService.sifreSifirlamaOnayla("abc", "YeniSifre123!");

        assertEquals("yeni-hash", k.getPassword());
        assertEquals(1L, k.getTokenVersion());
        assertTrue(token.getKullanildi());
        verify(sifreSifirlaTokenRepository).save(token);
    }
}
