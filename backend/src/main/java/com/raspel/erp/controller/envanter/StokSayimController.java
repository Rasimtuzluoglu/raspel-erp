package com.raspel.erp.controller.envanter;

import com.raspel.erp.dto.envanter.StokSayimDTO;
import com.raspel.erp.service.envanter.StokSayimService;
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

@Tag(name = "Stok Sayım", description = "Stok sayım yönetimi API")
@RestController
@RequestMapping("/api/stok-sayim")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class StokSayimController {

    private final StokSayimService stokSayimService;

    @GetMapping
    @Operation(summary = "Tüm stok sayımlarını getir", description = "Tüm stok sayım kayıtlarını listeler")
    public ResponseEntity<Page<StokSayimDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokSayimService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre stok sayım getir", description = "Stok sayım ID'sine göre detayları getirir")
    public ResponseEntity<StokSayimDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(stokSayimService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni stok sayımı oluştur", description = "Yeni bir stok sayım kaydı oluşturur")
    public ResponseEntity<StokSayimDTO> olustur(@Valid @RequestBody StokSayimDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(stokSayimService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Stok sayım güncelle", description = "Stok sayım kaydını günceller")
    public ResponseEntity<StokSayimDTO> guncelle(@PathVariable Long id, @Valid @RequestBody StokSayimDTO dto) {
        return ResponseEntity.ok(stokSayimService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Stok sayım sil", description = "Stok sayım kaydını siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        stokSayimService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
