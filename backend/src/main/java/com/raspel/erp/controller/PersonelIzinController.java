package com.raspel.erp.controller;

import com.raspel.erp.dto.PersonelIzinDTO;
import com.raspel.erp.service.PersonelIzinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/personel-izin")
@RequiredArgsConstructor
public class PersonelIzinController {

    private final PersonelIzinService personelIzinService;

    @GetMapping
    public ResponseEntity<List<PersonelIzinDTO>> tumu() {
        return ResponseEntity.ok(personelIzinService.tumunuGetir());
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
    public ResponseEntity<PersonelIzinDTO> olustur(@RequestBody PersonelIzinDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personelIzinService.olustur(dto));
    }

    @PutMapping("/{id}/durum")
    public ResponseEntity<PersonelIzinDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(personelIzinService.durumGuncelle(id, body.get("durum"), body.get("onaylayan")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        personelIzinService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
