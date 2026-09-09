package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.SohbetMesajDTO;
import com.raspel.erp.service.sistem.SohbetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Sohbet", description = "Ekip içi sohbet API")
@RestController
@RequestMapping("/api/sohbet")
@RequiredArgsConstructor
@Slf4j
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

    @PostMapping("/ai-ocr")
    @Operation(summary = "Fatura/Fiş OCR okuma", description = "Görüntüyü (base64) AI vision ile okuyup fatura kalemlerini JSON döndürür")
    public ResponseEntity<java.util.Map<String, Object>> aiOcr(@RequestBody java.util.Map<String, String> body, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        String base64Image = body != null ? body.get("gorsel") : "";
        String mimeType = body != null ? (body.get("mimeType") != null ? body.get("mimeType") : "image/jpeg") : "image/jpeg";
        String sonuc = sohbetService.aiOcrOku(base64Image, mimeType, sirketId);
        return ResponseEntity.ok(java.util.Map.of("sonuc", sonuc));
    }

    @GetMapping("/ai-sorgu-stream")
    @Operation(summary = "AI sorgusunu akışlı yanıtla", description = "Yapay zeka yanıtını token token (SSE) akıtır")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter aiSorguStream(
            @RequestParam String soru, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
                new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(0L);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                sohbetService.aiSorgulaStream(soru, sirketId, token -> {
                    try {
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .name("token").data(objectMapper.writeValueAsString(token)));
                    } catch (Exception e) {
                        log.warn("SSE token gönderilemedi: {}", e.getMessage());
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                log.error("AI akış sorgusu başarısız: {}", e.getMessage());
                try {
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .name("error").data(objectMapper.writeValueAsString("AI yanıtı alınamadı: " + e.getMessage())));
                } catch (Exception ignored) {
                }
                emitter.complete();
            } finally {
                executor.shutdown();
            }
        });
        return emitter;
    }
}
