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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.raspel.erp.entity.sube.Depo;
import com.raspel.erp.entity.envanter.Stok;

@Tag(name = "Depolar", description = "Depo yönetimi API")
@RestController
@RequestMapping("/api/depolar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class DepoController {

    private final DepoService depoService;

    @GetMapping
    @Operation(summary = "Tüm depoları getir", description = "Tüm depoları listeler")
    public ResponseEntity<Page<DepoDTO>> tumu(HttpServletRequest req, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        return ResponseEntity.ok(depoService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre depo getir", description = "Depo ID'sine göre detayları getirir")
    public ResponseEntity<DepoDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(depoService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni depo oluştur", description = "Yeni bir depo oluşturur")
    public ResponseEntity<DepoDTO> olustur(@Valid @RequestBody DepoDTO dto, HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        dto.setSirketId(sirketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(depoService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Depo güncelle", description = "Depo bilgilerini günceller")
    public ResponseEntity<DepoDTO> guncelle(@PathVariable Long id, @Valid @RequestBody DepoDTO dto) {
        return ResponseEntity.ok(depoService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Depo sil", description = "Depoyu siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        depoService.sil(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stoklar")
    @Operation(summary = "Depo stoklarını getir", description = "Belirli bir depodaki stokları listeler")
    public ResponseEntity<List<DepoStokDTO>> depoStoklari(@PathVariable Long id) {
        return ResponseEntity.ok(depoService.depoStoklari(id));
    }

    @PostMapping("/{id}/stok-ekle")
    @Operation(summary = "Depoya stok ekle", description = "Depoya stok girişi yapar")
    public ResponseEntity<DepoStokDTO> stokEkle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long stokId = Long.valueOf(body.get("stokId").toString());
        BigDecimal miktar = new BigDecimal(body.get("miktar").toString());
        return ResponseEntity.ok(depoService.stokEkle(id, stokId, miktar));
    }

    @PostMapping("/{id}/stok-cikar")
    @Operation(summary = "Depodan stok çıkar", description = "Depodan stok çıkışı yapar")
    public ResponseEntity<DepoStokDTO> stokCikar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long stokId = Long.valueOf(body.get("stokId").toString());
        BigDecimal miktar = new BigDecimal(body.get("miktar").toString());
        return ResponseEntity.ok(depoService.stokCikar(id, stokId, miktar));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Depolar arası stok transferi", description = "İki depo arasında stok transferi yapar")
    public ResponseEntity<Void> stokTransfer(@RequestBody Map<String, Object> body) {
        Long kaynakDepoId = Long.valueOf(body.get("kaynakDepoId").toString());
        Long hedefDepoId = Long.valueOf(body.get("hedefDepoId").toString());
        Long stokId = Long.valueOf(body.get("stokId").toString());
        BigDecimal miktar = new BigDecimal(body.get("miktar").toString());
        depoService.stokTransfer(kaynakDepoId, hedefDepoId, stokId, miktar);
        return ResponseEntity.ok().build();
    }
}