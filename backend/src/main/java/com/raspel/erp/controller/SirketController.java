package com.raspel.erp.controller;

import com.raspel.erp.dto.SirketDTO;
import com.raspel.erp.service.SirketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sirketler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class SirketController {

    private final SirketService sirketService;

    @GetMapping
    public ResponseEntity<List<SirketDTO>> tumu() {
        return ResponseEntity.ok(sirketService.tumunuGetir());
    }

    @GetMapping("/aktif")
    public ResponseEntity<List<SirketDTO>> aktifOlanlar() {
        return ResponseEntity.ok(sirketService.aktifOlanlariGetir());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SirketDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(sirketService.getir(id));
    }

    @PostMapping
    public ResponseEntity<SirketDTO> olustur(@RequestBody SirketDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sirketService.olustur(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SirketDTO> guncelle(@PathVariable Long id, @RequestBody SirketDTO dto) {
        return ResponseEntity.ok(sirketService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        sirketService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
