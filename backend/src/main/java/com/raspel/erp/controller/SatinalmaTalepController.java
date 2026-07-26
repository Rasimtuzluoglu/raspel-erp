package com.raspel.erp.controller;

import com.raspel.erp.dto.SatinalmaTalepDTO;
import com.raspel.erp.service.SatinalmaTalepService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/satinalma-talepler")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class SatinalmaTalepController {

    private final SatinalmaTalepService satinalmaTalepService;

    @GetMapping
    public ResponseEntity<List<SatinalmaTalepDTO>> tumu(@RequestParam(required = false) Long sirketId) {
        return ResponseEntity.ok(satinalmaTalepService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SatinalmaTalepDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(satinalmaTalepService.getir(id));
    }

    @PostMapping
    public ResponseEntity<SatinalmaTalepDTO> olustur(@Valid @RequestBody SatinalmaTalepDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(satinalmaTalepService.olustur(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SatinalmaTalepDTO> guncelle(@PathVariable Long id, @Valid @RequestBody SatinalmaTalepDTO dto) {
        return ResponseEntity.ok(satinalmaTalepService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    public ResponseEntity<SatinalmaTalepDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(satinalmaTalepService.durumGuncelle(id, body.get("durum")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        satinalmaTalepService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
