package com.raspel.erp.controller;

import com.raspel.erp.dto.PersonelIzinDTO;
import com.raspel.erp.service.PersonelIzinService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/personel-izin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class PersonelIzinController {

    private final PersonelIzinService personelIzinService;

    @GetMapping
    public ResponseEntity<List<PersonelIzinDTO>> tumu(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(personelIzinService.tumunuGetir(sirketId));
    }

    @GetMapping("/personel/{personelId}")
    public ResponseEntity<List<PersonelIzinDTO>> personelIzinleri(@PathVariable Long personelId) {
        return ResponseEntity.ok(personelIzinService.personelIzınleri(personelId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonelIzinDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(personelIzinService.getir(id));
    }

    @PostMapping
    public ResponseEntity<PersonelIzinDTO> olustur(@Valid @RequestBody PersonelIzinDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personelIzinService.olustur(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonelIzinDTO> guncelle(@PathVariable Long id, @Valid @RequestBody PersonelIzinDTO dto) {
        return ResponseEntity.ok(personelIzinService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    public ResponseEntity<PersonelIzinDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(personelIzinService.durumGuncelle(id, body.get("durum"), body.get("onaylayan")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        personelIzinService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
