package com.raspel.erp.controller.sube;

import com.raspel.erp.dto.sube.DepoDTO;
import com.raspel.erp.dto.sube.DepoStokDTO;
import com.raspel.erp.service.sube.DepoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/depolar")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class DepoController {

    private final DepoService depoService;

    @GetMapping
    public ResponseEntity<List<DepoDTO>> tumu(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        return ResponseEntity.ok(depoService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepoDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(depoService.getir(id));
    }

    @PostMapping
    public ResponseEntity<DepoDTO> olustur(@Valid @RequestBody DepoDTO dto, HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        dto.setSirketId(sirketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(depoService.olustur(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepoDTO> guncelle(@PathVariable Long id, @Valid @RequestBody DepoDTO dto) {
        return ResponseEntity.ok(depoService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        depoService.sil(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stoklar")
    public ResponseEntity<List<DepoStokDTO>> depoStoklari(@PathVariable Long id) {
        return ResponseEntity.ok(depoService.depoStoklari(id));
    }

    @PostMapping("/{id}/stok-ekle")
    public ResponseEntity<DepoStokDTO> stokEkle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long stokId = Long.valueOf(body.get("stokId").toString());
        BigDecimal miktar = new BigDecimal(body.get("miktar").toString());
        return ResponseEntity.ok(depoService.stokEkle(id, stokId, miktar));
    }

    @PostMapping("/{id}/stok-cikar")
    public ResponseEntity<DepoStokDTO> stokCikar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long stokId = Long.valueOf(body.get("stokId").toString());
        BigDecimal miktar = new BigDecimal(body.get("miktar").toString());
        return ResponseEntity.ok(depoService.stokCikar(id, stokId, miktar));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> stokTransfer(@RequestBody Map<String, Object> body) {
        Long kaynakDepoId = Long.valueOf(body.get("kaynakDepoId").toString());
        Long hedefDepoId = Long.valueOf(body.get("hedefDepoId").toString());
        Long stokId = Long.valueOf(body.get("stokId").toString());
        BigDecimal miktar = new BigDecimal(body.get("miktar").toString());
        depoService.stokTransfer(kaynakDepoId, hedefDepoId, stokId, miktar);
        return ResponseEntity.ok().build();
    }
}
