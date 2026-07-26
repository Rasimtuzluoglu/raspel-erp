package com.raspel.erp.controller.ik;

import com.raspel.erp.dto.ik.MaasBordroDTO;
import com.raspel.erp.service.ik.MaasBordroService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/maas-bordro")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class MaasBordroController {

    private final MaasBordroService maasBordroService;

    @GetMapping
    public ResponseEntity<List<MaasBordroDTO>> tumu(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(maasBordroService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaasBordroDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(maasBordroService.getir(id));
    }

    @PostMapping
    public ResponseEntity<MaasBordroDTO> olustur(@Valid @RequestBody MaasBordroDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(maasBordroService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaasBordroDTO> guncelle(@PathVariable Long id, @Valid @RequestBody MaasBordroDTO dto) {
        return ResponseEntity.ok(maasBordroService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        maasBordroService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
