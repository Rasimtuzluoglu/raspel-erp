package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.SatinalmaTalepDTO;
import com.raspel.erp.service.ticaret.SatinalmaTalepService;
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

@Tag(name = "Satın Alma Talepleri", description = "Satın alma talep yönetimi API")
@RestController
@RequestMapping("/api/satinalma-talepler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class SatinalmaTalepController {

    private final SatinalmaTalepService satinalmaTalepService;

    @GetMapping
    @Operation(summary = "Tüm satın alma taleplerini getir", description = "Tüm satın alma taleplerini listeler")
    public ResponseEntity<Page<SatinalmaTalepDTO>> tumu(@RequestParam(required = false) Long sirketId, @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(satinalmaTalepService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre satın alma talebi getir", description = "Satın alma talebi ID'sine göre detayları getirir")
    public ResponseEntity<SatinalmaTalepDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(satinalmaTalepService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni satın alma talebi oluştur", description = "Yeni bir satın alma talebi oluşturur")
    public ResponseEntity<SatinalmaTalepDTO> olustur(@Valid @RequestBody SatinalmaTalepDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(satinalmaTalepService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Satın alma talebi güncelle", description = "Satın alma talebi bilgilerini günceller")
    public ResponseEntity<SatinalmaTalepDTO> guncelle(@PathVariable Long id, @Valid @RequestBody SatinalmaTalepDTO dto) {
        return ResponseEntity.ok(satinalmaTalepService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    @Operation(summary = "Satın alma talebi durum güncelle", description = "Satın alma talebi durumunu günceller")
    public ResponseEntity<SatinalmaTalepDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(satinalmaTalepService.durumGuncelle(id, body.get("durum")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Satın alma talebi sil", description = "Satın alma talebini siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        satinalmaTalepService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
