package com.raspel.erp.controller.ik;

import com.raspel.erp.dto.ik.VardiyaDTO;
import com.raspel.erp.service.ik.VardiyaService;
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
import com.raspel.erp.entity.ik.Vardiya;

@Tag(name = "Vardiyalar", description = "Vardiya yönetimi API")
@RestController
@RequestMapping("/api/vardiyalar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class VardiyaController {

    private final VardiyaService vardiyaService;

    @GetMapping
    @Operation(summary = "Tüm vardiyaları getir", description = "Tüm vardiya kayıtlarını listeler")
    public ResponseEntity<Page<VardiyaDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(vardiyaService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/ozet")
    @Operation(summary = "Vardiya KPI özeti",
            description = "Toplam/bugün/bu hafta vardiya sayıları ve vardiyası olan personel sayısı")
    public ResponseEntity<com.raspel.erp.dto.ik.VardiyaOzetDTO> ozet(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(vardiyaService.ozet(sirketId));
    }

    @GetMapping("/filtreli")
    @Operation(summary = "Vardiya ara/filtrele (sayfalı)",
            description = "Personel, tarih aralığı, tür ve personel adı araması")
    public ResponseEntity<Page<VardiyaDTO>> filtreli(
            HttpServletRequest request,
            @RequestParam(required = false) Long personelId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate baslangic,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bitis,
            @RequestParam(required = false) String tur,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 50, sort = "tarih", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(vardiyaService.filtreli(sirketId, personelId, baslangic, bitis, tur, q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre vardiya getir", description = "Vardiya ID'sine göre detayları getirir")
    public ResponseEntity<VardiyaDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(vardiyaService.getir(id));
    }

    @GetMapping("/personel/{personelId}")
    @Operation(summary = "Personel vardiyalarını getir", description = "Belirli bir personelin tüm vardiya kayıtlarını listeler")
    public ResponseEntity<java.util.List<VardiyaDTO>> personelVardiyalari(@PathVariable Long personelId) {
        return ResponseEntity.ok(vardiyaService.personelVardiyalari(personelId));
    }

    @PostMapping
    @Operation(summary = "Yeni vardiya oluştur", description = "Yeni bir vardiya kaydı oluşturur")
    public ResponseEntity<VardiyaDTO> olustur(@Valid @RequestBody VardiyaDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(vardiyaService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Vardiya güncelle", description = "Vardiya bilgilerini günceller")
    public ResponseEntity<VardiyaDTO> guncelle(@PathVariable Long id, @Valid @RequestBody VardiyaDTO dto) {
        return ResponseEntity.ok(vardiyaService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Vardiya sil", description = "Vardiyayı siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        vardiyaService.sil(id);
        return ResponseEntity.noContent().build();
    }
}