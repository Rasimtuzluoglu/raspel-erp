package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.PushAbonelikDTO;
import com.raspel.erp.service.sistem.WebPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Push", description = "PWA Web Push abonelik API")
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class PushSubscriptionController {

    private final WebPushService webPushService;

    @GetMapping("/vapid-public-key")
    @Operation(summary = "VAPID public key", description = "Tarayici abonelik icin kullanilacak VAPID public key'i dondurur")
    public ResponseEntity<Map<String, Object>> vapidPublicKey() {
        return ResponseEntity.ok(Map.of(
                "aktif", webPushService.aktif(),
                "publicKey", webPushService.publicKey() != null ? webPushService.publicKey() : ""));
    }

    @PostMapping("/abone")
    @Operation(summary = "Push aboneligi kaydet", description = "Tarayici PushSubscription bilgisini kaydeder/gunceller")
    public ResponseEntity<PushAbonelikDTO> abone(
            @RequestBody PushAbonelikDTO dto,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String userAgent = request.getHeader("User-Agent");
        return ResponseEntity.ok(webPushService.aboneOl(dto, kullaniciId, sirketId, userAgent));
    }

    @DeleteMapping("/abone")
    @Operation(summary = "Push aboneligi sil", description = "Endpoint'e ait aboneligi siler")
    public ResponseEntity<Void> aboneSil(@RequestParam String endpoint) {
        webPushService.aboneSil(endpoint);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    @Operation(summary = "Test bildirimi", description = "Mevcut sirkete ornek push bildirimi gonderir")
    public ResponseEntity<Map<String, Object>> test(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        int gonderilen = webPushService.gonder(sirketId, "RasPel ERP",
                "Test bildirimi basariyla gonderildi.", "/");
        return ResponseEntity.ok(Map.of("gonderilen", gonderilen, "aktif", webPushService.aktif()));
    }
}
