package com.raspel.erp.controller;

import com.raspel.erp.dto.StokDTO;
import com.raspel.erp.dto.StokHareketDTO;
import com.raspel.erp.service.StokService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stoklar")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class StokController {

    private final StokService stokService;

    @GetMapping
    public ResponseEntity<Page<StokDTO>> tumu(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokService.tumunuGetir(sirketId, PageRequest.of(page, size)));
    }

    @GetMapping("/ara")
    public ResponseEntity<List<StokDTO>> ara(@RequestParam String q) { return ResponseEntity.ok(stokService.ara(q)); }

    @GetMapping("/{id}")
    public ResponseEntity<StokDTO> getir(@PathVariable Long id) { return ResponseEntity.ok(stokService.getir(id)); }

    @PostMapping
    public ResponseEntity<StokDTO> olustur(@RequestBody @jakarta.validation.Valid StokDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(stokService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StokDTO> guncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid StokDTO dto) {
        return ResponseEntity.ok(stokService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) { stokService.sil(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/{id}/hareketler")
    public ResponseEntity<List<StokHareketDTO>> hareketler(@PathVariable Long id) {
        return ResponseEntity.ok(stokService.hareketler(id));
    }

    @GetMapping("/hareketler/tum")
    public ResponseEntity<List<StokHareketDTO>> tumHareketler() {
        return ResponseEntity.ok(stokService.tumHareketler());
    }

    @PostMapping("/{id}/hareketler")
    public ResponseEntity<StokHareketDTO> hareketEkle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid StokHareketDTO dto) {
        dto.setStokId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(stokService.hareketEkle(dto));
    }

    @DeleteMapping("/hareketler/{hareketId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hareketSil(@PathVariable Long hareketId) {
        stokService.hareketSil(hareketId);
        return ResponseEntity.noContent().build();
    }
}
