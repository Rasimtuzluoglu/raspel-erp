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
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE', 'DRIVER')")
public class SohbetController {

    private final SohbetService sohbetService;
    private final com.raspel.erp.service.sistem.AiService aiService;

    /** SSE akisinin azami suresi (2 dk). */
    private static final long SSE_TIMEOUT_MS = 120_000L;

    /**
     * Sinirli, paylasilan SSE havuzu (AsyncConfig). Test dilimlerinde bean
     * bulunmayabilir; o durumda gorev cagiran thread'de calisir.
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("sohbetSseExecutor")
    private java.util.concurrent.Executor sseExecutor;

    @GetMapping("/ai-dashboard-ozet")
    @Operation(summary = "Dashboard AI özeti",
            description = "Şirketin güncel verilerinden yapay zeka ile kısa bir yönetici özeti üretir (AI yapılandırılmış olmalıdır)")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
    public ResponseEntity<java.util.Map<String, String>> aiDashboardOzet(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        String baglam = sohbetService.veriBaglami(sirketId);
        String ozet = aiService.sorgula(sirketId,
                "Sen bir ERP yönetici asistanısın. Verilen şirket verilerine dayanarak EN FAZLA 3 cümlelik, "
                        + "Türkçe, net ve aksiyon önerisi içeren bir yönetici özeti yaz. Uydurma veri ekleme.",
                baglam);
        return ResponseEntity.ok(java.util.Map.of("ozet", ozet));
    }

    @GetMapping
    @Operation(summary = "Son sohbet mesajlarını getir",
            description = "Genel sohbetin son 50 mesajını getirir. Daha eski mesajlar için cursor (ilk mesajın id'si) ve limit verilir.")
    public ResponseEntity<List<SohbetMesajDTO>> sonMesajlar(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(sohbetService.sonMesajlar(sirketId, cursor, limit));
    }

    @GetMapping("/okunmamis")
    @Operation(summary = "Genel sohbette okunmamış mesaj sayısı",
            description = "Kullanıcının sunucuda saklanan son okuma işaretinden sonraki genel sohbet mesajlarını sayar.")
    public ResponseEntity<java.util.Map<String, Long>> genelOkunmamis(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(java.util.Map.of(
                "okunmamis", sohbetService.genelOkunmamisSayisi(sirketId, kullaniciId)));
    }

    @PostMapping("/okundu")
    @Operation(summary = "Genel sohbeti okundu işaretle",
            description = "Kullanıcının genel sohbette gördüğü son mesaj id'sini kaydeder.")
    public ResponseEntity<Void> genelOkundu(@RequestBody java.util.Map<String, Long> body, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        sohbetService.genelOkunduIsaretle(sirketId, kullaniciId, body != null ? body.get("sonOkunanId") : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Operation(summary = "Sohbet mesajı gönder", description = "Yeni bir sohbet mesajı gönderir")
    public ResponseEntity<SohbetMesajDTO> mesajGonder(@Valid @RequestBody SohbetMesajDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String displayName = (String) request.getAttribute("displayName");
        return ResponseEntity.ok(sohbetService.mesajGonder(dto, sirketId, kullaniciId, displayName));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Sohbet mesajını sil", description = "Genel sohbet mesajını siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> mesajSil(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("DELETE /api/sohbet/{} - Mesaj siliniyor, sirketId: {}", id, sirketId);
        sohbetService.mesajSil(id, sirketId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/dosya")
    @Operation(summary = "Genel sohbete dosya yükle", description = "Genel sohbette paylaşılmak üzere dosya/görsel yükler")
    public ResponseEntity<java.util.Map<String, String>> dosyaYukle(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(java.util.Map.of("url", sohbetService.dosyaYukle(file)));
    }

    @PostMapping("/ai-sorgu")
    @Operation(summary = "Doğal dilde veri sorgulama (AI Asistan)", description = "Yapay zeka ile doğal dil analitiği ve görsel grafik yanıtı üretir")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
    public ResponseEntity<com.raspel.erp.dto.sistem.AISorguSonucDTO> aiSorgu(@RequestBody java.util.Map<String, String> body, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String soru = body != null ? body.get("soru") : "";
        return ResponseEntity.ok(sohbetService.aiSorgula(soru, sirketId, kullaniciId));
    }

    @PostMapping("/ai-ocr")
    @Operation(summary = "Fatura/Fiş OCR okuma", description = "Görüntüyü (base64) AI vision ile okuyup fatura kalemlerini JSON döndürür")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
    public ResponseEntity<java.util.Map<String, Object>> aiOcr(@RequestBody java.util.Map<String, String> body, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String base64Image = body != null ? body.get("gorsel") : "";
        String mimeType = body != null ? (body.get("mimeType") != null ? body.get("mimeType") : "image/jpeg") : "image/jpeg";
        String sonuc = sohbetService.aiOcrOku(base64Image, mimeType, sirketId, kullaniciId);
        return ResponseEntity.ok(java.util.Map.of("sonuc", sonuc));
    }

    @GetMapping("/ai-sorgu-stream")
    @Operation(summary = "AI sorgusunu akışlı yanıtla", description = "Yapay zeka yanıtını token token (SSE) akıtır")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter aiSorguStream(
            @RequestParam String soru, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        // Zaman asimi ve paylasilan havuz: her istek icin yeni thread acmak
        // (Executors.newSingleThreadExecutor + 0L timeout) kimlik dogrulamis
        // bir kullanicinin thread/memory tuketmesine (DoS) yol aciyordu.
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
                new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(SSE_TIMEOUT_MS);
        java.util.concurrent.atomic.AtomicBoolean kapandi = new java.util.concurrent.atomic.AtomicBoolean(false);
        emitter.onCompletion(() -> kapandi.set(true));
        emitter.onTimeout(() -> {
            kapandi.set(true);
            emitter.complete();
        });
        emitter.onError(e -> kapandi.set(true));
        Runnable gorev = () -> {
            try {
                sohbetService.aiSorgulaStream(soru, sirketId, kullaniciId, token -> {
                    if (kapandi.get()) return;
                    try {
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .name("token").data(objectMapper.writeValueAsString(token)));
                    } catch (Exception e) {
                        kapandi.set(true);
                        log.debug("SSE token gönderilemedi (istemci ayrıldı): {}", e.getMessage());
                    }
                });
            } catch (Exception e) {
                log.error("AI akış sorgusu başarısız: {}", e.getMessage());
                if (!kapandi.get()) {
                    try {
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .name("error").data(objectMapper.writeValueAsString(
                                        "AI yanıtı alınamadı: " + e.getMessage())));
                    } catch (Exception ignored) {
                    }
                }
            } finally {
                kapandi.set(true);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
        };
        try {
            if (sseExecutor != null) {
                sseExecutor.execute(gorev);
            } else {
                gorev.run();
            }
        } catch (java.util.concurrent.RejectedExecutionException e) {
            log.warn("SSE havuzu dolu, istek reddedildi (kullaniciId={})", kullaniciId);
            try {
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("error").data(objectMapper.writeValueAsString(
                                "Şu anda çok fazla AI isteği var, lütfen biraz sonra tekrar deneyin.")));
            } catch (Exception ignored) {
            }
            emitter.complete();
        }
        return emitter;
    }
}
