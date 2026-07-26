package com.raspel.erp.controller;

import com.raspel.erp.dto.KasaDTO;
import com.raspel.erp.dto.KasaHareketDTO;
import com.raspel.erp.service.KasaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kasalar")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class KasaController {

    private final KasaService kasaService;

    @GetMapping
    public ResponseEntity<List<KasaDTO>> tumu(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(kasaService.tumKasalarGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KasaDTO> getir(@PathVariable Long id) { return ResponseEntity.ok(kasaService.kasaGetir(id)); }

    @PostMapping
    public ResponseEntity<KasaDTO> olustur(@RequestBody @jakarta.validation.Valid KasaDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(kasaService.kasaOlustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KasaDTO> guncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid KasaDTO dto) {
        return ResponseEntity.ok(kasaService.kasaGuncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) { kasaService.kasaSil(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/{id}/hareketler")
    public ResponseEntity<List<KasaHareketDTO>> hareketler(@PathVariable Long id) {
        return ResponseEntity.ok(kasaService.kasaHareketleriGetir(id));
    }

    @PostMapping("/{id}/hareketler")
    public ResponseEntity<KasaHareketDTO> hareketEkle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid KasaHareketDTO dto) {
        dto.setKasaId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(kasaService.hareketEkle(dto));
    }

    @DeleteMapping("/hareketler/{hareketId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hareketSil(@PathVariable Long hareketId) {
        kasaService.hareketSil(hareketId);
        return ResponseEntity.noContent().build();
    }
}
