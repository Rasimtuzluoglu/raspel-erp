package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.LoginResponse;
import com.raspel.erp.service.sistem.QrOturumService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Telefonla QR oturum onayi uclari.
 */
@RestController
@RequestMapping("/api/kullanicilar")
@RequiredArgsConstructor
public class QrOturumController {

    private final QrOturumService qrOturumService;

    @PostMapping("/qr-oturum")
    public ResponseEntity<Map<String, Object>> baslat(@RequestBody Map<String, String> govde,
                                                       HttpServletRequest request) {
        String girisToken = govde != null ? govde.get("girisToken") : null;
        String ip = istemciIp(request);
        String userAgent = request.getHeader("User-Agent");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(qrOturumService.baslat(girisToken, ip, userAgent));
    }

    @GetMapping("/qr-oturum/{token}")
    public ResponseEntity<Map<String, Object>> durum(@PathVariable String token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(qrOturumService.durum(token));
    }

    @GetMapping(value = "/qr-oturum/{token}/gorsel", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> gorsel(@PathVariable String token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG)
                .body(qrOturumService.gorsel(token));
    }

    @PostMapping("/qr-oturum/{token}/onayla")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'DRIVER')")
    public ResponseEntity<Void> onayla(@PathVariable String token, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        qrOturumService.onayla(token, kullaniciId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/qr-oturum/{token}/reddet")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'DRIVER')")
    public ResponseEntity<Void> reddet(@PathVariable String token, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        qrOturumService.reddet(token, kullaniciId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/giris-qr-tamamla")
    public ResponseEntity<LoginResponse> tamamla(@RequestBody Map<String, String> govde) {
        String girisToken = govde != null ? govde.get("girisToken") : null;
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(qrOturumService.tamamla(girisToken));
    }

    private String istemciIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
