package com.raspel.erp.controller;

import com.raspel.erp.dto.PersonelPuantajDTO;
import com.raspel.erp.service.PersonelPuantajService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Personel Puantaj", description = "Personel puantaj yönetimi API")
@RestController
@RequestMapping("/api/personel-puantaj")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class PersonelPuantajController {

    private final PersonelPuantajService personelPuantajService;

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
