package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.IadeDTO;
import com.raspel.erp.service.ticaret.IadeService;
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

@Tag(name = "İadeler", description = "İade yönetimi API")
@RestController
@RequestMapping("/api/iadeler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class IadeController {

    private final IadeService iadeService;

    @GetMapping
    @Operation(summary = "Tüm iadeleri getir", description = "Tüm iade kayıtlarını listeler")
    public ResponseEntity<Page<IadeDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(iadeService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre iade getir", description = "İade ID'sine göre detayları getirir")
    public ResponseEntity<IadeDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(iadeService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni iade oluştur", description = "Yeni bir iade kaydı oluşturur")
    public ResponseEntity<IadeDTO> olustur(@Valid @RequestBody IadeDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(iadeService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "İade güncelle", description = "İade bilgilerini günceller")
    public ResponseEntity<IadeDTO> guncelle(@PathVariable Long id, @Valid @RequestBody IadeDTO dto) {
        return ResponseEntity.ok(iadeService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    @Operation(summary = "İade durum güncelle", description = "İade durumunu günceller (TASLAK/TAMAMLANDI/IPTAL)")
    public ResponseEntity<IadeDTO> durumGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid com.raspel.erp.dto.sistem.DurumGuncelleRequest body) {
        return ResponseEntity.ok(iadeService.durumGuncelle(id, body.getDurum()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "İade sil", description = "İade kaydını siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        iadeService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
