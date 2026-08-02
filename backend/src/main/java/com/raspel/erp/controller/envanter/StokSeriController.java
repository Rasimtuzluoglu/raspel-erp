package com.raspel.erp.controller.envanter;

import com.raspel.erp.dto.envanter.StokSeriDTO;
import com.raspel.erp.service.envanter.StokSeriService;
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

@Tag(name = "Stok Seri/Lot", description = "Stok seri ve lot numarası yönetimi API")
@RestController
@RequestMapping("/api/stok-seri")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class StokSeriController {

    private final StokSeriService stokSeriService;

    @GetMapping
    @Operation(summary = "Tüm seri/lotları getir", description = "Tüm stok seri/lot kayıtlarını listeler")
    public ResponseEntity<Page<StokSeriDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokSeriService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/stok/{stokId}")
    @Operation(summary = "Stoğa göre seri/lot getir", description = "Belirli bir stoğa ait seri/lot numaralarını listeler")
    public ResponseEntity<List<StokSeriDTO>> stokIcinGetir(@PathVariable Long stokId) {
        return ResponseEntity.ok(stokSeriService.stokIcinGetir(stokId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre seri/lot getir", description = "Seri/lot ID'sine göre detayları getirir")
    public ResponseEntity<StokSeriDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(stokSeriService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni seri/lot oluştur", description = "Yeni bir stok seri/lot kaydı oluşturur")
    public ResponseEntity<StokSeriDTO> olustur(@Valid @RequestBody StokSeriDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stokSeriService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Seri/lot güncelle", description = "Seri/lot kaydını günceller")
    public ResponseEntity<StokSeriDTO> guncelle(@PathVariable Long id, @Valid @RequestBody StokSeriDTO dto) {
        return ResponseEntity.ok(stokSeriService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Seri/lot sil", description = "Seri/lot kaydını siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        stokSeriService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
