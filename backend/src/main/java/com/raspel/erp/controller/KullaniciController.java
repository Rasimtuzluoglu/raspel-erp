package com.raspel.erp.controller;

import com.raspel.erp.dto.KullaniciDTO;
import com.raspel.erp.dto.LoginRequest;
import com.raspel.erp.dto.LoginResponse;
import com.raspel.erp.dto.SifreDegistirRequest;
import com.raspel.erp.service.KullaniciService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

@Tag(name = "Kullanıcılar", description = "Kullanıcı yönetimi ve kimlik doğrulama API")
@RestController
@RequestMapping("/api/kullanicilar")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", allowCredentials = "true")
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
