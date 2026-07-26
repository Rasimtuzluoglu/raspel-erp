package com.raspel.erp.controller;

import com.raspel.erp.dto.PersonelDTO;
import com.raspel.erp.service.PersonelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personel")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class PersonelController {

    private final PersonelService personelService;

    @GetMapping
    public ResponseEntity<List<PersonelDTO>> tumu(@RequestParam(required = false) Long sirketId) {
        return ResponseEntity.ok(personelService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonelDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(personelService.getir(id));
    }

    @PostMapping
    public ResponseEntity<PersonelDTO> olustur(@RequestBody PersonelDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personelService.olustur(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonelDTO> guncelle(@PathVariable Long id, @RequestBody PersonelDTO dto) {
        return ResponseEntity.ok(personelService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        personelService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
