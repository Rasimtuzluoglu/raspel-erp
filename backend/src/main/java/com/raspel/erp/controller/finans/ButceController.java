package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.ButceDTO;
import com.raspel.erp.service.finans.ButceService;
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

@Tag(name = "Bütçeler", description = "Bütçe yönetimi API")
@RestController
@RequestMapping("/api/butceler")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class ButceController {

    private final ButceService butceService;

    @GetMapping
    @Operation(summary = "Tüm bütçeleri getir", description = "Tüm bütçe kayıtlarını listeler")
    public ResponseEntity<Page<ButceDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(butceService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre bütçe getir", description = "Bütçe ID'sine göre detayları getirir")
    public ResponseEntity<ButceDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(butceService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni bütçe oluştur", description = "Yeni bir bütçe kaydı oluşturur")
    public ResponseEntity<ButceDTO> olustur(@Valid @RequestBody ButceDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(butceService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Bütçe güncelle", description = "Bütçe bilgilerini günceller")
    public ResponseEntity<ButceDTO> guncelle(@PathVariable Long id, @Valid @RequestBody ButceDTO dto) {
        return ResponseEntity.ok(butceService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Bütçe sil", description = "Bütçeyi siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        butceService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
