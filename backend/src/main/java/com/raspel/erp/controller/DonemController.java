package com.raspel.erp.controller;

import com.raspel.erp.dto.DonemDTO;
import com.raspel.erp.service.DonemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Dönemler", description = "Dönem yönetimi API")
@RestController
@RequestMapping("/api/donemler")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class DonemController {

    private final DonemService donemService;

    @GetMapping
    @Operation(summary = "Tüm dönemleri getir", description = "Tüm dönemleri listeler")
    public ResponseEntity<Page<DonemDTO>> tumu(@PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(donemService.tumunuGetir(pageable));
    }

    @GetMapping("/sirket/{sirketId}")
    @Operation(summary = "Şirkete göre dönemleri getir", description = "Belirli bir şirkete ait dönemleri listeler")
    public ResponseEntity<List<DonemDTO>> sirketeGore(@PathVariable Long sirketId) {
        return ResponseEntity.ok(donemService.sirketeGoreGetir(sirketId));
    }

    @GetMapping("/sirket/{sirketId}/aktif")
    @Operation(summary = "Aktif dönemleri getir", description = "Şirketin aktif dönemlerini listeler")
    public ResponseEntity<List<DonemDTO>> aktifDonemler(@PathVariable Long sirketId) {
        return ResponseEntity.ok(donemService.aktifDonemler(sirketId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre dönem getir", description = "Dönem ID'sine göre detayları getirir")
    public ResponseEntity<DonemDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(donemService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni dönem oluştur", description = "Yeni bir dönem oluşturur")
    public ResponseEntity<DonemDTO> olustur(@Valid @RequestBody DonemDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donemService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Dönem güncelle", description = "Dönem bilgilerini günceller")
    public ResponseEntity<DonemDTO> guncelle(@PathVariable Long id, @Valid @RequestBody DonemDTO dto) {
        return ResponseEntity.ok(donemService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dönem sil", description = "Dönemi siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        donemService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
