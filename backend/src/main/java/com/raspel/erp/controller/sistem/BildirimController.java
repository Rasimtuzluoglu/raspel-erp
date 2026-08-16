package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.BildirimDTO;
import com.raspel.erp.service.sistem.BildirimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Bildirimler", description = "Bildirim merkezi API")
@RestController
@RequestMapping("/api/bildirimler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class BildirimController {

    private final BildirimService bildirimService;

    @GetMapping
    @Operation(summary = "Bildirimleri getir", description = "Şirketin son 50 bildirimini getirir")
    public ResponseEntity<List<BildirimDTO>> liste(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(bildirimService.liste(sirketId));
    }

    @GetMapping("/okunmamis")
    @Operation(summary = "Okunmamış bildirim sayısı", description = "Okunmamış bildirim sayısını getirir")
    public ResponseEntity<Map<String, Object>> okunmamis(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(Map.of("adet", bildirimService.okunmamisSayisi(sirketId)));
    }

    @PutMapping("/{id}/okundu")
    @Operation(summary = "Bildirimi okundu işaretle", description = "Belirtilen bildirimi okundu olarak işaretler")
    public ResponseEntity<Void> okundu(@PathVariable Long id) {
        bildirimService.okunduIsaretle(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/tumu-okundu")
    @Operation(summary = "Tümünü okundu işaretle", description = "Şirketin tüm bildirimlerini okundu olarak işaretler")
    public ResponseEntity<Void> tumuOkundu(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        bildirimService.tumunuOkunduIsaretle(sirketId);
        return ResponseEntity.ok().build();
    }
}
