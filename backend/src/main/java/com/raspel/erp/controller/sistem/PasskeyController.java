package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.*;
import com.raspel.erp.service.sistem.PasskeyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Passkey (WebAuthn) kaydi ve parolasiz giris uclari.
 */
@RestController
@RequestMapping("/api/kullanicilar")
@RequiredArgsConstructor
public class PasskeyController {

    private final PasskeyService passkeyService;

    @PostMapping("/passkey/kayit/secenekler")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'DRIVER')")
    public ResponseEntity<PasskeySecenekDTO> kayitSecenekleri(
            @RequestBody(required = false) PasskeyKayitSecenekRequest req,
            HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(passkeyService.kayitSecenekleri(kullaniciId,
                req != null ? req.getPassword() : null));
    }

    @PostMapping("/passkey/kayit")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'DRIVER')")
    public ResponseEntity<PasskeyDTO> kaydet(@RequestBody PasskeyKayitRequest req,
                                              HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(passkeyService.kaydet(kullaniciId, req));
    }

    @GetMapping("/passkeyler")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'DRIVER')")
    public ResponseEntity<List<PasskeyDTO>> listele(HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(passkeyService.listele(kullaniciId));
    }

    @DeleteMapping("/passkeyler/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'DRIVER')")
    public ResponseEntity<Void> sil(@PathVariable Long id, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        passkeyService.sil(kullaniciId, id);
        return ResponseEntity.noContent().build();
    }

    /** ADMIN: kullanicinin tum passkey'lerini sifirlar. */
    @DeleteMapping("/{id}/passkeyler")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminSil(@PathVariable Long id) {
        passkeyService.adminSil(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/passkey/giris/secenekler")
    public ResponseEntity<PasskeySecenekDTO> girisSecenekleri() {
        return ResponseEntity.ok(passkeyService.girisSecenekleri());
    }

    @PostMapping("/passkey/giris")
    public ResponseEntity<LoginResponse> giris(@RequestBody PasskeyGirisRequest req) {
        return ResponseEntity.ok(passkeyService.giris(req));
    }
}
