package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.FiyatListesiDTO;
import com.raspel.erp.service.ticaret.FiyatListesiService;
import jakarta.servlet.http.HttpServletRequest;
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

@Tag(name = "Fiyat Listesi", description = "Fiyat listesi yönetimi API")
@RestController
@RequestMapping("/api/fiyat-listesi")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class FiyatListesiController {

    private final FiyatListesiService fiyatListesiService;

    @GetMapping
    @Operation(summary = "Tüm fiyat listelerini getir", description = "Tüm fiyat listelerini listeler")
    public ResponseEntity<Page<FiyatListesiDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(fiyatListesiService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre fiyat listesi getir", description = "Fiyat listesi ID'sine göre detayları getirir")
    public ResponseEntity<FiyatListesiDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(fiyatListesiService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni fiyat listesi oluştur", description = "Yeni bir fiyat listesi oluşturur")
    public ResponseEntity<FiyatListesiDTO> olustur(@Valid @RequestBody FiyatListesiDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(fiyatListesiService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Fiyat listesi güncelle", description = "Fiyat listesi bilgilerini günceller")
    public ResponseEntity<FiyatListesiDTO> guncelle(@PathVariable Long id, @Valid @RequestBody FiyatListesiDTO dto) {
        return ResponseEntity.ok(fiyatListesiService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Fiyat listesi sil", description = "Fiyat listesini siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        fiyatListesiService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
