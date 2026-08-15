package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.SatinalmaSiparisDTO;
import com.raspel.erp.service.ticaret.SatinalmaSiparisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@Tag(name = "Satın Alma Siparişleri", description = "Satın alma sipariş yönetimi API")
@RestController
@RequestMapping("/api/satinalma-siparisler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class SatinalmaSiparisController {

    private final SatinalmaSiparisService satinalmaSiparisService;

    @GetMapping
    @Operation(summary = "Tüm satın alma siparişlerini getir", description = "Tüm satın alma siparişlerini listeler")
    public ResponseEntity<Page<SatinalmaSiparisDTO>> tumu(@RequestParam(required = false) Long sirketId, @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(satinalmaSiparisService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre satın alma siparişi getir", description = "Satın alma siparişi ID'sine göre detayları getirir")
    public ResponseEntity<SatinalmaSiparisDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(satinalmaSiparisService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni satın alma siparişi oluştur", description = "Yeni bir satın alma siparişi oluşturur")
    public ResponseEntity<SatinalmaSiparisDTO> olustur(@Valid @RequestBody SatinalmaSiparisDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(satinalmaSiparisService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Satın alma siparişi güncelle", description = "Satın alma siparişi bilgilerini günceller")
    public ResponseEntity<SatinalmaSiparisDTO> guncelle(@PathVariable Long id, @Valid @RequestBody SatinalmaSiparisDTO dto) {
        return ResponseEntity.ok(satinalmaSiparisService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    @Operation(summary = "Satın alma siparişi durum güncelle", description = "Satın alma siparişi durumunu günceller")
    public ResponseEntity<SatinalmaSiparisDTO> durumGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid com.raspel.erp.dto.sistem.DurumGuncelleRequest body) {
        return ResponseEntity.ok(satinalmaSiparisService.durumGuncelle(id, body.getDurum()));
    }

    @PostMapping("/{id}/faturaya-cevir")
    @Operation(summary = "Alış faturasına dönüştür", description = "Satın alma siparişini alış faturasına dönüştürür (stok artar)")
    public ResponseEntity<com.raspel.erp.dto.ticaret.FaturaDTO> faturayaCevir(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String displayName = (String) request.getAttribute("displayName");
        return ResponseEntity.ok(satinalmaSiparisService.faturayaCevir(id, kullaniciId, displayName));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Satın alma siparişi sil", description = "Satın alma siparişini siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        satinalmaSiparisService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
