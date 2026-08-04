package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.BankaDTO;
import com.raspel.erp.service.finans.BankaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.raspel.erp.entity.finans.Banka;

@Tag(name = "Bankalar", description = "Banka yönetimi API")
@RestController
@RequestMapping("/api/bankalar")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class BankaController {

    private final BankaService bankaService;

    @GetMapping
    @Operation(summary = "Tüm bankaları getir", description = "Şirkete ait tüm banka hesaplarını listeler")
    public ResponseEntity<Page<BankaDTO>> tumBankalariGetir(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(bankaService.tumBankalariGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre banka getir", description = "Banka ID'sine göre detayları getirir")
    public ResponseEntity<BankaDTO> bankaGetir(@PathVariable Long id) {
        return ResponseEntity.ok(bankaService.bankaGetir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni banka oluştur", description = "Yeni bir banka hesabı oluşturur")
    public ResponseEntity<BankaDTO> bankaOlustur(@RequestBody @jakarta.validation.Valid BankaDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(bankaService.bankaOlustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Banka güncelle", description = "Banka bilgilerini günceller")
    public ResponseEntity<BankaDTO> bankaGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid BankaDTO dto) {
        return ResponseEntity.ok(bankaService.bankaGuncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Banka sil", description = "Bankayı siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> bankaSil(@PathVariable Long id) {
        bankaService.bankaSil(id);
        return ResponseEntity.noContent().build();
    }
}