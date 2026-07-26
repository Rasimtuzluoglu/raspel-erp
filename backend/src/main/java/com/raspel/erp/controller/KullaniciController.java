package com.raspel.erp.controller;

import com.raspel.erp.dto.KullaniciDTO;
import com.raspel.erp.dto.LoginRequest;
import com.raspel.erp.dto.LoginResponse;
import com.raspel.erp.service.KullaniciService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/kullanicilar")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", allowCredentials = "true")
public class KullaniciController {

    private final KullaniciService kullaniciService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<KullaniciDTO>> tumu() { return ResponseEntity.ok(kullaniciService.tumunuGetir()); }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KullaniciDTO> getir(@PathVariable Long id) { return ResponseEntity.ok(kullaniciService.getir(id)); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KullaniciDTO> olustur(@RequestBody @jakarta.validation.Valid KullaniciDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kullaniciService.olustur(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KullaniciDTO> guncelle(@PathVariable Long id, @Valid @RequestBody KullaniciDTO dto) {
        return ResponseEntity.ok(kullaniciService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) { kullaniciService.sil(id); return ResponseEntity.noContent().build(); }

    @PostMapping("/giris")
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
