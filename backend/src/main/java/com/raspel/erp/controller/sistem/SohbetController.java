package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.SohbetMesajDTO;
import com.raspel.erp.service.sistem.SohbetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Sohbet", description = "Ekip içi sohbet API")
@RestController
@RequestMapping("/api/sohbet")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class SohbetController {

    private final SohbetService sohbetService;

    @GetMapping
    @Operation(summary = "Son sohbet mesajlarını getir", description = "Şirketin son 50 sohbet mesajını getirir")
    public ResponseEntity<List<SohbetMesajDTO>> sonMesajlar(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(sohbetService.sonMesajlar(sirketId));
    }

    @PostMapping
    @Operation(summary = "Sohbet mesajı gönder", description = "Yeni bir sohbet mesajı gönderir")
    public ResponseEntity<SohbetMesajDTO> mesajGonder(@Valid @RequestBody SohbetMesajDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String displayName = (String) request.getAttribute("displayName");
        return ResponseEntity.ok(sohbetService.mesajGonder(dto, sirketId, kullaniciId, displayName));
    }

    @PostMapping("/ai-sorgu")
    @Operation(summary = "Doğal dilde veri sorgulama (AI Asistan)", description = "Yapay zeka ile doğal dil analitiği ve görsel grafik yanıtı üretir")
    public ResponseEntity<com.raspel.erp.dto.sistem.AISorguSonucDTO> aiSorgu(@RequestBody java.util.Map<String, String> body, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        String soru = body != null ? body.get("soru") : "";
        return ResponseEntity.ok(sohbetService.aiSorgula(soru, sirketId));
    }
}
