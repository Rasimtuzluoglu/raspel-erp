package com.raspel.erp.controller;

import com.raspel.erp.dto.SiparisDTO;
import com.raspel.erp.service.SiparisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/siparisler")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class SiparisController {

    private final SiparisService siparisService;

    @GetMapping
    public ResponseEntity<List<SiparisDTO>> tumu(@RequestParam(required = false) Long sirketId) {
        return ResponseEntity.ok(siparisService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SiparisDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(siparisService.getir(id));
    }

    @PostMapping
    public ResponseEntity<SiparisDTO> olustur(@Valid @RequestBody SiparisDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siparisService.olustur(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SiparisDTO> guncelle(@PathVariable Long id, @Valid @RequestBody SiparisDTO dto) {
        return ResponseEntity.ok(siparisService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    public ResponseEntity<SiparisDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(siparisService.durumGuncelle(id, body.get("durum")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        siparisService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
