package com.raspel.erp.controller;

import com.raspel.erp.dto.PersonelPuantajDTO;
import com.raspel.erp.service.PersonelPuantajService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/personel-puantaj")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class PersonelPuantajController {

    private final PersonelPuantajService personelPuantajService;

    @GetMapping("/personel/{personelId}")
    public ResponseEntity<List<PersonelPuantajDTO>> personelPuantajlari(
            @PathVariable Long personelId,
            @RequestParam LocalDate baslangic,
            @RequestParam LocalDate bitis) {
        return ResponseEntity.ok(personelPuantajService.personelPuantajlari(personelId, baslangic, bitis));
    }

    @PostMapping
    public ResponseEntity<PersonelPuantajDTO> olustur(@Valid @RequestBody PersonelPuantajDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personelPuantajService.olustur(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonelPuantajDTO> guncelle(@PathVariable Long id, @Valid @RequestBody PersonelPuantajDTO dto) {
        return ResponseEntity.ok(personelPuantajService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        personelPuantajService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
