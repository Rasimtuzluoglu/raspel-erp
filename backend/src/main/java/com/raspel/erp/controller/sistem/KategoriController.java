package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.KategoriDTO;
import com.raspel.erp.service.sistem.KategoriService;
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

@Tag(name = "Kategoriler", description = "Kategori yönetimi API")
@RestController
@RequestMapping("/api/kategoriler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class KategoriController {

    private final KategoriService kategoriService;

    @GetMapping
    @Operation(summary = "Tüm kategorileri getir", description = "Şirkete ait tüm kategorileri listeler")
    public ResponseEntity<Page<KategoriDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(kategoriService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/tur/{tur}")
    @Operation(summary = "Türe göre kategorileri getir", description = "Belirli bir türe ait kategorileri listeler")
    public ResponseEntity<List<KategoriDTO>> turuGetir(@PathVariable String tur, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(kategoriService.turuGetir(tur, sirketId));
    }

    @PostMapping
    @Operation(summary = "Yeni kategori oluştur", description = "Yeni bir kategori oluşturur")
    public ResponseEntity<KategoriDTO> olustur(@RequestBody @jakarta.validation.Valid KategoriDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(kategoriService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Kategori güncelle", description = "Kategori bilgilerini günceller")
    public ResponseEntity<KategoriDTO> guncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid KategoriDTO dto) {
        return ResponseEntity.ok(kategoriService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kategori sil", description = "Kategoriyi siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) { kategoriService.sil(id); return ResponseEntity.noContent().build(); }
}
