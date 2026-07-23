package com.raspel.erp.controller.ik;

import com.raspel.erp.dto.ik.VardiyaDTO;
import com.raspel.erp.service.ik.VardiyaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vardiyalar")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
public class VardiyaController {

    private final VardiyaService vardiyaService;

    @GetMapping
    public ResponseEntity<List<VardiyaDTO>> tumu(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(vardiyaService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VardiyaDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(vardiyaService.getir(id));
    }

    @PostMapping
    public ResponseEntity<VardiyaDTO> olustur(@RequestBody VardiyaDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(vardiyaService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VardiyaDTO> guncelle(@PathVariable Long id, @RequestBody VardiyaDTO dto) {
        return ResponseEntity.ok(vardiyaService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        vardiyaService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
