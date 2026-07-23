package com.raspel.erp.controller;

import com.raspel.erp.dto.PersonelPuantajDTO;
import com.raspel.erp.service.PersonelPuantajService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/personel-puantaj")
@RequiredArgsConstructor
public class PersonelPuantajController {

    private final PersonelPuantajService personelPuantajService;

    @GetMapping("/personel/{personelId}")
    public ResponseEntity<List<PersonelPuantajDTO>> personelPuantajlari(
            @PathVariable Long personelId,
            @RequestParam LocalDate baslangic,
            @RequestParam LocalDate bitis) {
        return ResponseEntity.ok(personelPuantajService.personelPuantajlari(personelId, baslangic, bitis));
    }

    @PostMapping
    public ResponseEntity<PersonelPuantajDTO> olustur(@RequestBody PersonelPuantajDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personelPuantajService.olustur(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        personelPuantajService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
