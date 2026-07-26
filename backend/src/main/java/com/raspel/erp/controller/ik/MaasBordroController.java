package com.raspel.erp.controller.ik;

import com.raspel.erp.dto.ik.MaasBordroDTO;
import com.raspel.erp.service.ik.MaasBordroService;
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

@Tag(name = "Maaş Bordro", description = "Maaş bordro yönetimi API")
@RestController
@RequestMapping("/api/maas-bordro")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class MaasBordroController {

    private final MaasBordroService maasBordroService;

    @GetMapping
    @Operation(summary = "Tüm maaş bordrolarını getir", description = "Tüm maaş bordro kayıtlarını listeler")
    public ResponseEntity<Page<MaasBordroDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(maasBordroService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre maaş bordrosu getir", description = "Maaş bordrosu ID'sine göre detayları getirir")
    public ResponseEntity<MaasBordroDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(maasBordroService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni maaş bordrosu oluştur", description = "Yeni bir maaş bordrosu oluşturur")
    public ResponseEntity<MaasBordroDTO> olustur(@Valid @RequestBody MaasBordroDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(maasBordroService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Maaş bordrosu güncelle", description = "Maaş bordrosu bilgilerini günceller")
    public ResponseEntity<MaasBordroDTO> guncelle(@PathVariable Long id, @Valid @RequestBody MaasBordroDTO dto) {
        return ResponseEntity.ok(maasBordroService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Maaş bordrosu sil", description = "Maaş bordrosunu siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        maasBordroService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
