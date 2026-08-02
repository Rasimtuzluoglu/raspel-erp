package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.MasrafDTO;
import com.raspel.erp.service.finans.MasrafService;
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

@Tag(name = "Masraflar", description = "Masraf yönetimi API")
@RestController
@RequestMapping("/api/masraflar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class MasrafController {

    private final MasrafService masrafService;

    @GetMapping
    @Operation(summary = "Tüm masrafları getir", description = "Tüm masraf kayıtlarını listeler")
    public ResponseEntity<Page<MasrafDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(masrafService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre masraf getir", description = "Masraf ID'sine göre detayları getirir")
    public ResponseEntity<MasrafDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(masrafService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni masraf oluştur", description = "Yeni bir masraf kaydı oluşturur")
    public ResponseEntity<MasrafDTO> olustur(@Valid @RequestBody MasrafDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(masrafService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Masraf güncelle", description = "Masraf bilgilerini günceller")
    public ResponseEntity<MasrafDTO> guncelle(@PathVariable Long id, @Valid @RequestBody MasrafDTO dto) {
        return ResponseEntity.ok(masrafService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Masraf sil", description = "Masraf kaydını siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        masrafService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
