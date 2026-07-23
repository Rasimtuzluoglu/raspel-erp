package com.raspel.erp.controller.sube;

import com.raspel.erp.dto.sube.SubeDTO;
import com.raspel.erp.service.sube.SubeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subeler")
@RequiredArgsConstructor
public class SubeController {

    private final SubeService subeService;

    @GetMapping
    public ResponseEntity<List<SubeDTO>> tumu(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        return ResponseEntity.ok(subeService.tumunuGetir(sirketId));
    }

    @GetMapping("/aktif")
    public ResponseEntity<List<SubeDTO>> aktifSubeler(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        return ResponseEntity.ok(subeService.aktifSubeler(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubeDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(subeService.getir(id));
    }

    @PostMapping
    public ResponseEntity<SubeDTO> olustur(@RequestBody SubeDTO dto, HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        dto.setSirketId(sirketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(subeService.olustur(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubeDTO> guncelle(@PathVariable Long id, @RequestBody SubeDTO dto) {
        return ResponseEntity.ok(subeService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        subeService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
