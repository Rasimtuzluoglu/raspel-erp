package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.SiparisDTO;
import com.raspel.erp.service.ticaret.SiparisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.util.Map;

@Tag(name = "Siparişler", description = "Sipariş yönetimi API")
@RestController
@RequestMapping("/api/siparisler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class SiparisController {

    private final SiparisService siparisService;

    @GetMapping
    @Operation(summary = "Tüm siparişleri getir", description = "Tüm siparişleri listeler")
    public ResponseEntity<Page<SiparisDTO>> tumu(@RequestParam(required = false) Long sirketId, @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(siparisService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre sipariş getir", description = "Sipariş ID'sine göre detayları getirir")
    public ResponseEntity<SiparisDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(siparisService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni sipariş oluştur", description = "Yeni bir sipariş oluşturur")
    public ResponseEntity<SiparisDTO> olustur(@Valid @RequestBody SiparisDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siparisService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sipariş güncelle", description = "Sipariş bilgilerini günceller")
    public ResponseEntity<SiparisDTO> guncelle(@PathVariable Long id, @Valid @RequestBody SiparisDTO dto) {
        return ResponseEntity.ok(siparisService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    @Operation(summary = "Sipariş durum güncelle", description = "Sipariş durumunu günceller")
    public ResponseEntity<SiparisDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(siparisService.durumGuncelle(id, body.get("durum")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Sipariş sil", description = "Siparişi siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        siparisService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
