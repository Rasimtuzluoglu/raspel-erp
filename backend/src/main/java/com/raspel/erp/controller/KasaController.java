package com.raspel.erp.controller;

import com.raspel.erp.dto.KasaDTO;
import com.raspel.erp.dto.KasaHareketDTO;
import com.raspel.erp.service.KasaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Kasalar", description = "Kasa yönetimi API")
@RestController
@RequestMapping("/api/kasalar")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class KasaController {

    private final KasaService kasaService;

    @GetMapping
    @Operation(summary = "Tüm kasaları getir", description = "Şirkete ait tüm kasaları listeler")
    public ResponseEntity<Page<KasaDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(kasaService.tumKasalarGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre kasa getir", description = "Kasa ID'sine göre detayları getirir")
    public ResponseEntity<KasaDTO> getir(@PathVariable Long id) { return ResponseEntity.ok(kasaService.kasaGetir(id)); }

    @PostMapping
    @Operation(summary = "Yeni kasa oluştur", description = "Yeni bir kasa oluşturur")
    public ResponseEntity<KasaDTO> olustur(@RequestBody @jakarta.validation.Valid KasaDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(kasaService.kasaOlustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Kasa güncelle", description = "Kasa bilgilerini günceller")
    public ResponseEntity<KasaDTO> guncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid KasaDTO dto) {
        return ResponseEntity.ok(kasaService.kasaGuncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kasa sil", description = "Kasayı siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) { kasaService.kasaSil(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/{id}/hareketler")
    @Operation(summary = "Kasa hareketlerini getir", description = "Belirli bir kasaya ait hareketleri listeler")
    public ResponseEntity<List<KasaHareketDTO>> hareketler(@PathVariable Long id) {
        return ResponseEntity.ok(kasaService.kasaHareketleriGetir(id));
    }

    @PostMapping("/{id}/hareketler")
    @Operation(summary = "Kasa hareketi ekle", description = "Kasaya yeni bir hareket (giriş/çıkış) ekler")
    public ResponseEntity<KasaHareketDTO> hareketEkle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid KasaHareketDTO dto) {
        dto.setKasaId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(kasaService.hareketEkle(dto));
    }

    @DeleteMapping("/hareketler/{hareketId}")
    @Operation(summary = "Kasa hareketi sil", description = "Kasa hareketini siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hareketSil(@PathVariable Long hareketId) {
        kasaService.hareketSil(hareketId);
        return ResponseEntity.noContent().build();
    }
}
