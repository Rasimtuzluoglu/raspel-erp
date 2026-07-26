package com.raspel.erp.controller;

import com.raspel.erp.dto.CekSenetDTO;
import com.raspel.erp.service.CekSenetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cek-senet")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class CekSenetController {

    private final CekSenetService cekSenetService;

    @GetMapping
    public ResponseEntity<List<CekSenetDTO>> tumu(@RequestParam(required = false) Long sirketId) {
        return ResponseEntity.ok(cekSenetService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CekSenetDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(cekSenetService.getir(id));
    }

    @PostMapping
    public ResponseEntity<CekSenetDTO> olustur(@RequestBody CekSenetDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cekSenetService.olustur(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CekSenetDTO> guncelle(@PathVariable Long id, @RequestBody CekSenetDTO dto) {
        return ResponseEntity.ok(cekSenetService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    public ResponseEntity<CekSenetDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(cekSenetService.durumGuncelle(id, body.get("durum")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        cekSenetService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
