package com.raspel.erp.controller.muhasebe;

import com.raspel.erp.dto.muhasebe.IrsaliyeDTO;
import com.raspel.erp.service.muhasebe.IrsaliyeService;
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
import java.util.Map;

@Tag(name = "İrsaliyeler", description = "İrsaliye yönetimi API")
@RestController
@RequestMapping("/api/irsaliyeler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class IrsaliyeController {

    private final IrsaliyeService irsaliyeService;

    @GetMapping
    @Operation(summary = "Tüm irsaliyeleri getir", description = "Tüm irsaliyeleri listeler")
    public ResponseEntity<Page<IrsaliyeDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(irsaliyeService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre irsaliye getir", description = "İrsaliye ID'sine göre detayları getirir")
    public ResponseEntity<IrsaliyeDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(irsaliyeService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni irsaliye oluştur", description = "Yeni bir irsaliye oluşturur")
    public ResponseEntity<IrsaliyeDTO> olustur(HttpServletRequest request, @Valid @RequestBody IrsaliyeDTO dto) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(irsaliyeService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "İrsaliye güncelle", description = "İrsaliye bilgilerini günceller")
    public ResponseEntity<IrsaliyeDTO> guncelle(@PathVariable Long id, @Valid @RequestBody IrsaliyeDTO dto) {
        return ResponseEntity.ok(irsaliyeService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    @Operation(summary = "İrsaliye durum güncelle", description = "İrsaliye durumunu günceller")
    public ResponseEntity<IrsaliyeDTO> durumGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid com.raspel.erp.dto.sistem.DurumGuncelleRequest body) {
        return ResponseEntity.ok(irsaliyeService.durumGuncelle(id, body.getDurum()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "İrsaliye sil", description = "İrsaliyeyi siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        irsaliyeService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
