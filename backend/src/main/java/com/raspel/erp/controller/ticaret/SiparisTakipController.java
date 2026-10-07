package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.SiparisTakipDTO;
import com.raspel.erp.service.ticaret.SiparisTakipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Sipariş Takibi", description = "Sipariş → Üretim → Sevk → Teslimat zinciri API")
@RestController
@RequestMapping("/api/siparis-takip")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class SiparisTakipController {

    private final SiparisTakipService takipService;

    @GetMapping
    @Operation(summary = "Sipariş zincirini getir",
            description = "Sipariş → üretim → sevk → teslimat zincirini filtreli ve sayfalı döndürür.")
    public ResponseEntity<Page<SiparisTakipDTO>> zincir(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) String sofor,
            @PageableDefault(size = 25) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(takipService.filtreliZincir(sirketId, q, durum, sofor, pageable));
    }

    @GetMapping("/soforler")
    @Operation(summary = "Şoför listesini getir", description = "Filtre için mevcut şoför adlarını döndürür.")
    public ResponseEntity<List<String>> soforler(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(takipService.soforler(sirketId));
    }
}
