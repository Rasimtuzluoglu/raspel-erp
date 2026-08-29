package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.TahsilatDTO;
import com.raspel.erp.service.finans.TahsilatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@Tag(name = "Tahsilat", description = "Tahsilat ve alacak yönetimi API")
@RestController
@RequestMapping("/api/tahsilat")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class TahsilatController {

    private final TahsilatService tahsilatService;

    @GetMapping
    @Operation(summary = "Tahsilat özeti", description = "Ödenmemiş alacakların cari bazlı yaşlandırma özetini getirir")
    public ResponseEntity<TahsilatDTO> ozet(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(tahsilatService.ozetGetir(sirketId));
    }

    @PostMapping("/{cariId}/hatirlat")
    @Operation(summary = "Hatırlatma gönder", description = "Cariye ait ödenmemiş faturalar için e-posta hatırlatması gönderir")
    public ResponseEntity<Map<String, Object>> hatirlat(HttpServletRequest request, @PathVariable Long cariId) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        int gonderilen = tahsilatService.hatirlat(cariId, sirketId);
        return ResponseEntity.ok(Map.of("gonderilen", gonderilen));
    }
}
