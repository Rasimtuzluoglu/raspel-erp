package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.AiConfigDTO;
import com.raspel.erp.service.sistem.AiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AI Yapilandirma", description = "Yapay Zeka API Key yonetimi")
@RestController
@RequestMapping("/api/ai-config")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class AiConfigController {

    private final AiConfigService aiConfigService;

    @GetMapping
    @Operation(summary = "AI yapilandirmasini getir")
    public ResponseEntity<AiConfigDTO> getConfig(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(aiConfigService.getConfig(sirketId));
    }

    @PostMapping
    @Operation(summary = "AI yapilandirmasini kaydet")
    public ResponseEntity<AiConfigDTO> saveConfig(@RequestBody AiConfigDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(aiConfigService.saveConfig(dto, sirketId));
    }

    @PostMapping("/test")
    @Operation(summary = "AI baglanti testi")
    public ResponseEntity<Map<String, Object>> testConnection(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(aiConfigService.testConnection(sirketId));
    }

    @DeleteMapping
    @Operation(summary = "AI yapilandirmasini sil")
    public ResponseEntity<Void> deleteConfig(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        aiConfigService.deleteConfig(sirketId);
        return ResponseEntity.noContent().build();
    }
}
