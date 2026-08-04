package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.KullaniciDTO;
import com.raspel.erp.dto.sistem.LoginRequest;
import com.raspel.erp.dto.sistem.LoginResponse;
import com.raspel.erp.dto.sistem.SifreDegistirRequest;
import com.raspel.erp.service.sistem.KullaniciService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import com.raspel.erp.dto.sistem.SifreSifirlaRequest;
import com.raspel.erp.dto.sistem.TwoFactorDTO;
import com.raspel.erp.dto.sistem.TwoFactorGirisRequest;

@Tag(name = "Kullanıcılar", description = "Kullanıcı yönetimi ve kimlik doğrulama API")
@RestController
@RequestMapping("/api/kullanicilar")
@RequiredArgsConstructor
@Slf4j
public class KullaniciController {

    private final KullaniciService kullaniciService;

    @GetMapping
    @Operation(summary = "Tüm kullanıcıları getir", description = "Tüm kullanıcıları listeler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<KullaniciDTO>> tumu(@PageableDefault(size = 50) Pageable pageable) { return ResponseEntity.ok(kullaniciService.tumunuGetir(pageable)); }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre kullanıcı getir", description = "Kullanıcı ID'sine göre detayları getirir (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KullaniciDTO> getir(@PathVariable Long id) { return ResponseEntity.ok(kullaniciService.getir(id)); }

    @PostMapping
    @Operation(summary = "Yeni kullanıcı oluştur", description = "Yeni bir kullanıcı oluşturur (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KullaniciDTO> olustur(@RequestBody @jakarta.validation.Valid KullaniciDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kullaniciService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Kullanıcı güncelle", description = "Kullanıcı bilgilerini günceller (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KullaniciDTO> guncelle(@PathVariable Long id, @Valid @RequestBody KullaniciDTO dto) {
        return ResponseEntity.ok(kullaniciService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kullanıcı sil", description = "Kullanıcıyı siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) { kullaniciService.sil(id); return ResponseEntity.noContent().build(); }

    @PutMapping("/sifre-degistir")
    @Operation(summary = "Şifre değiştir", description = "Oturum açmış kullanıcının şifresini değiştirir")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> sifreDegistir(@Valid @RequestBody SifreDegistirRequest req, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        kullaniciService.sifreDegistir(kullaniciId, req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sifre-sifirla")
    @Operation(summary = "Şifre sıfırla", description = "Belirtilen kullanıcının şifresini sıfırlar (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sifreSifirla(@Valid @RequestBody com.raspel.erp.dto.sistem.SifreSifirlaRequest req) {
        kullaniciService.sifreSifirla(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/setup-2fa")
    @Operation(summary = "2FA Kurulumu", description = "Oturum açmış kullanıcı için gerçek TOTP secret ve otpauth URI üretir")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<com.raspel.erp.dto.sistem.TwoFactorDTO> setup2FA(HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(kullaniciService.setupTwoFactor(kullaniciId));
    }

    @PostMapping("/enable-2fa")
    @Operation(summary = "2FA Etkinleştir", description = "TOTP doğrulama kodunu kontrol edip 2FA'yi aktif eder")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> enable2FA(@RequestBody com.raspel.erp.dto.sistem.TwoFactorDTO dto, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        kullaniciService.enableTwoFactor(kullaniciId, dto.getCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/disable-2fa")
    @Operation(summary = "2FA Devre Dışı", description = "TOTP kodunu doğrulayıp 2FA'yi kapatır")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> disable2FA(@RequestBody com.raspel.erp.dto.sistem.TwoFactorDTO dto, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        kullaniciService.disableTwoFactor(kullaniciId, dto.getCode());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ben")
    @Operation(summary = "Mevcut kullanıcı", description = "Oturum açmış kullanıcının bilgilerini getirir")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<KullaniciDTO> ben(HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(kullaniciService.getir(kullaniciId));
    }

    @PutMapping("/ben")
    @Operation(summary = "Profil güncelle", description = "Oturum açmış kullanıcının kendi profilini güncellemesine izin verir")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<KullaniciDTO> beniGuncelle(@Valid @RequestBody KullaniciDTO dto, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(kullaniciService.profilGuncelle(kullaniciId, dto));
    }

    @PostMapping("/giris-2fa")
    @Operation(summary = "2FA giriş adımı", description = "Kullanıcı adı/şifre sonrası 2FA kodunu doğrulayıp JWT token döndürür")
    public ResponseEntity<LoginResponse> giris2fa(@RequestBody @jakarta.validation.Valid com.raspel.erp.dto.sistem.TwoFactorGirisRequest req,
                                                  HttpServletResponse response) {
        LoginResponse loginResponse = kullaniciService.giris2faTamamla(req);
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", loginResponse.getToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/")
                .maxAge(86400)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/giris")
    @Operation(summary = "Kullanıcı girişi", description = "Kullanıcı adı ve şifre ile giriş yapar, JWT token döndürür")
    public ResponseEntity<LoginResponse> giris(@RequestBody @jakarta.validation.Valid LoginRequest req,
                                                HttpServletResponse response) {
        LoginResponse loginResponse = kullaniciService.giris(req);
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", loginResponse.getToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/")
                .maxAge(86400)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        return ResponseEntity.ok(loginResponse);
    }
}