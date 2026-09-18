package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.IskontoKuraliDTO;
import com.raspel.erp.service.ticaret.IskontoMotoruService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Tag(name = "İskonto Motoru", description = "Kademeli fiyat/iskonto kuralları API")
@RestController
@RequestMapping("/api/iskonto-kurallari")
@RequiredArgsConstructor
public class IskontoKuraliController {

    private final IskontoMotoruService iskontoMotoruService;

    @GetMapping
    @Operation(summary = "İskonto kurallarını getir", description = "Şirketin kademeli iskonto kurallarını listeler")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
    public ResponseEntity<Page<IskontoKuraliDTO>> tumu(HttpServletRequest request,
                                                       @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(iskontoMotoruService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Kural getir", description = "ID'ye göre iskonto kuralını getirir")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
    public ResponseEntity<IskontoKuraliDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(iskontoMotoruService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Kural oluştur", description = "Yeni bir kademeli iskonto kuralı oluşturur")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<IskontoKuraliDTO> olustur(@Valid @RequestBody IskontoKuraliDTO dto,
                                                    HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        if (sirketId != null) dto.setSirketId(sirketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(iskontoMotoruService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Kural güncelle", description = "İskonto kuralını günceller")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<IskontoKuraliDTO> guncelle(@PathVariable Long id,
                                                     @Valid @RequestBody IskontoKuraliDTO dto) {
        return ResponseEntity.ok(iskontoMotoruService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kural sil", description = "İskonto kuralını siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        iskontoMotoruService.sil(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/hesapla")
    @Operation(summary = "İskonto hesapla", description = "Verilen satır için uygulanacak iskonto oranını hesaplar")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
    public ResponseEntity<Map<String, Object>> hesapla(
            HttpServletRequest request,
            @RequestParam(required = false) Long stokId,
            @RequestParam(required = false) Long cariHesapId,
            @RequestParam(required = false) String kategori,
            @RequestParam(required = false) BigDecimal adet,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate tarih) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        BigDecimal oran = iskontoMotoruService.iskontoHesapla(sirketId, stokId, cariHesapId, kategori, adet, tarih);
        return ResponseEntity.ok(Map.of("iskontoOrani", oran));
    }
}
