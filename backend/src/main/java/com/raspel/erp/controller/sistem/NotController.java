package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.NotDTO;
import com.raspel.erp.service.sistem.NotService;
import com.raspel.erp.entity.sistem.Kullanici;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.raspel.erp.entity.sistem.Not;

@Tag(name = "Notlar", description = "Not alma ve yönetme API")
@RestController
@RequestMapping("/api/notlar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class NotController {

    private final NotService notService;

    @GetMapping
    @Operation(summary = "Tüm notları getir", description = "Şirkete ait tüm notları listeler")
    public ResponseEntity<Page<NotDTO>> tumu(HttpServletRequest request,
                                             @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(notService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Not detayı", description = "ID'ye göre not detayını getirir")
    public ResponseEntity<NotDTO> idyeGore(@PathVariable Long id) {
        return ResponseEntity.ok(notService.idyeGoreGetir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni not oluştur", description = "Yeni bir not oluşturur")
    public ResponseEntity<NotDTO> olustur(@Valid @RequestBody NotDTO dto,
                                           HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notService.olustur(dto, sirketId, kullaniciId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Not güncelle", description = "Not bilgilerini günceller")
    public ResponseEntity<NotDTO> guncelle(@PathVariable Long id,
                                            @Valid @RequestBody NotDTO dto) {
        return ResponseEntity.ok(notService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Not sil", description = "Notu siler")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        notService.sil(id);
        return ResponseEntity.noContent().build();
    }
}