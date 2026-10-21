package com.raspel.erp.controller.ik;

import com.raspel.erp.dto.ik.PersonelPuantajDTO;
import com.raspel.erp.service.ik.PersonelPuantajService;
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
import java.time.LocalDate;
import java.util.List;
import com.raspel.erp.entity.ik.Personel;

@Tag(name = "Personel Puantaj", description = "Personel puantaj yönetimi API")
@RestController
@RequestMapping("/api/personel-puantaj")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class PersonelPuantajController {

    private final PersonelPuantajService personelPuantajService;

    @GetMapping("/ozet")
    @Operation(summary = "Puantaj KPI özeti",
            description = "Şirket genelinde geldi/gelmedi/izinli/mazeretli puantaj kayıt sayıları")
    public ResponseEntity<com.raspel.erp.dto.ik.PuantajOzetDTO> ozet(jakarta.servlet.http.HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(personelPuantajService.ozet(sirketId));
    }

    @GetMapping("/filtreli")
    @Operation(summary = "Puantaj ara/filtrele (sayfalı)",
            description = "Şirket geneli puantaj; personel, tarih aralığı, durum ve personel adı araması")
    public ResponseEntity<Page<PersonelPuantajDTO>> filtreli(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam(required = false) Long personelId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate baslangic,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bitis,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 50, sort = "tarih", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(personelPuantajService.filtreli(sirketId, personelId, baslangic, bitis, durum, q, pageable));
    }

    @GetMapping("/personel/{personelId}")
    @Operation(summary = "Personel puantajlarını getir", description = "Belirli bir personelin belirli tarih aralığındaki puantajlarını getirir")
    public ResponseEntity<List<PersonelPuantajDTO>> personelPuantajlari(
            @PathVariable Long personelId,
            @RequestParam LocalDate baslangic,
            @RequestParam LocalDate bitis) {
        return ResponseEntity.ok(personelPuantajService.personelPuantajlari(personelId, baslangic, bitis));
    }

    @PostMapping
    @Operation(summary = "Yeni puantaj oluştur", description = "Yeni bir personel puantaj kaydı oluşturur")
    public ResponseEntity<PersonelPuantajDTO> olustur(@Valid @RequestBody PersonelPuantajDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personelPuantajService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Puantaj güncelle", description = "Puantaj kaydını günceller")
    public ResponseEntity<PersonelPuantajDTO> guncelle(@PathVariable Long id, @Valid @RequestBody PersonelPuantajDTO dto) {
        return ResponseEntity.ok(personelPuantajService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Puantaj sil", description = "Puantaj kaydını siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        personelPuantajService.sil(id);
        return ResponseEntity.noContent().build();
    }
}