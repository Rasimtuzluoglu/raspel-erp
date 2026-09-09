package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.SiparisTakipDTO;
import com.raspel.erp.service.ticaret.SiparisTakipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
    @Operation(summary = "Sipariş zincirini getir")
    public ResponseEntity<List<SiparisTakipDTO>> zincir(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(takipService.zincir(sirketId));
    }
}
