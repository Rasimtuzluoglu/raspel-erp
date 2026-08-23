package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.SirketDTO;
import com.raspel.erp.service.sistem.SirketService;
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

@Tag(name = "Şirketler", description = "Şirket yönetimi API")
@RestController
@RequestMapping("/api/sirketler")
@RequiredArgsConstructor
public class SirketController {

    private final SirketService sirketService;

    @GetMapping
    @Operation(summary = "Tüm şirketleri getir", description = "Tüm şirket kayıtlarını listeler")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<SirketDTO>> tumu(@PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(sirketService.tumunuGetir(pageable));
    }

    @GetMapping("/aktif")
    @Operation(summary = "Aktif şirketleri getir", description = "Aktif durumdaki şirketleri listeler")
    public ResponseEntity<List<SirketDTO>> aktifOlanlar() {
        return ResponseEntity.ok(sirketService.aktifOlanlariGetir());
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre şirket getir", description = "Şirket ID'sine göre detayları getirir")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SirketDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(sirketService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni şirket oluştur", description = "Yeni bir şirket kaydı oluşturur")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SirketDTO> olustur(@Valid @RequestBody SirketDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sirketService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Şirket güncelle", description = "Şirket bilgilerini günceller")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SirketDTO> guncelle(@PathVariable Long id, @Valid @RequestBody SirketDTO dto) {
        return ResponseEntity.ok(sirketService.guncelle(id, dto));
    }

    @GetMapping("/{id}/konsolide-ozet")
    @Operation(summary = "Konsolide grup şirket özeti", description = "Ana şirket ve bağlı alt şirketlerin toplam stok, bakiye ve ciro özetini getirir")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<com.raspel.erp.dto.sistem.KonsolideOzetDTO> konsolideOzet(@PathVariable Long id) {
        return ResponseEntity.ok(sirketService.konsolideOzet(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Şirket sil", description = "Şirket kaydını siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        sirketService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
