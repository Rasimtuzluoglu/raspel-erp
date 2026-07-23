package com.raspel.erp.controller;

import com.raspel.erp.dto.KategoriDTO;
import com.raspel.erp.service.KategoriService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kategoriler")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
public class KategoriController {

    private final KategoriService kategoriService;

    @GetMapping
    public ResponseEntity<List<KategoriDTO>> tumu(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(kategoriService.tumunuGetir(sirketId));
    }

    @GetMapping("/tur/{tur}")
    public ResponseEntity<List<KategoriDTO>> turuGetir(@PathVariable String tur) {
        return ResponseEntity.ok(kategoriService.turuGetir(tur));
    }

    @PostMapping
    public ResponseEntity<KategoriDTO> olustur(@RequestBody @jakarta.validation.Valid KategoriDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(kategoriService.olustur(dto, sirketId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> sil(@PathVariable Long id) { kategoriService.sil(id); return ResponseEntity.noContent().build(); }
}
