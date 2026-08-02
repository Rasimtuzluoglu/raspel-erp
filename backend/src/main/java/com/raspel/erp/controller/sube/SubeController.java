package com.raspel.erp.controller.sube;

import com.raspel.erp.dto.sube.SubeDTO;
import com.raspel.erp.service.sube.SubeService;
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
import java.util.List;

@Tag(name = "Şubeler", description = "Şube yönetimi API")
@RestController
@RequestMapping("/api/subeler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class SubeController {

    private final SubeService subeService;

    @GetMapping
    @Operation(summary = "Tüm şubeleri getir", description = "Tüm şubeleri listeler")
    public ResponseEntity<Page<SubeDTO>> tumu(HttpServletRequest req, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        return ResponseEntity.ok(subeService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/aktif")
    @Operation(summary = "Aktif şubeleri getir", description = "Aktif durumdaki şubeleri listeler")
    public ResponseEntity<List<SubeDTO>> aktifSubeler(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        return ResponseEntity.ok(subeService.aktifSubeler(sirketId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre şube getir", description = "Şube ID'sine göre detayları getirir")
    public ResponseEntity<SubeDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(subeService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni şube oluştur", description = "Yeni bir şube oluşturur")
    public ResponseEntity<SubeDTO> olustur(@Valid @RequestBody SubeDTO dto, HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        dto.setSirketId(sirketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(subeService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Şube güncelle", description = "Şube bilgilerini günceller")
    public ResponseEntity<SubeDTO> guncelle(@PathVariable Long id, @Valid @RequestBody SubeDTO dto) {
        return ResponseEntity.ok(subeService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Şube sil", description = "Şubeyi siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        subeService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
