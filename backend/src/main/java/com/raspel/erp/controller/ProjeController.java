package com.raspel.erp.controller;

import com.raspel.erp.dto.GorevDTO;
import com.raspel.erp.dto.ProjeDTO;
import com.raspel.erp.service.ProjeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projeler")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class ProjeController {

    private final ProjeService projeService;

    @GetMapping
    public ResponseEntity<List<ProjeDTO>> tumu(@RequestParam(required = false) Long sirketId) {
        return ResponseEntity.ok(projeService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjeDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(projeService.getir(id));
    }

    @PostMapping
    public ResponseEntity<ProjeDTO> olustur(@Valid @RequestBody ProjeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projeService.olustur(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjeDTO> guncelle(@PathVariable Long id, @Valid @RequestBody ProjeDTO dto) {
        return ResponseEntity.ok(projeService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    public ResponseEntity<ProjeDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(projeService.durumGuncelle(id, body.get("durum")));
    }

    @PutMapping("/gorev/{gorevId}/durum")
    public ResponseEntity<GorevDTO> gorevDurumGuncelle(@PathVariable Long gorevId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(projeService.gorevDurumGuncelle(gorevId, body.get("durum")));
    }

    @PostMapping("/{projeId}/gorevler")
    public ResponseEntity<ProjeDTO> gorevEkle(@PathVariable Long projeId, @Valid @RequestBody GorevDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projeService.gorevEkle(projeId, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        projeService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
