package com.raspel.erp.controller;

import com.raspel.erp.dto.StokDTO;
import com.raspel.erp.dto.StokHareketDTO;
import com.raspel.erp.dto.KritikStokDTO;
import com.raspel.erp.service.StokService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Stoklar", description = "Stok yönetimi API")
@RestController
@RequestMapping("/api/stoklar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class StokController {

    private final StokService stokService;

    @GetMapping
    @Operation(summary = "Tüm stokları getir (sayfalı)", description = "Tüm stokları sayfalı olarak listeler")
    public ResponseEntity<Page<StokDTO>> tumu(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokService.tumunuGetir(sirketId, PageRequest.of(page, size)));
    }

    @GetMapping("/ara")
    @Operation(summary = "Stok ara", description = "Stokları ada/barkoda göre arar")
    public ResponseEntity<List<StokDTO>> ara(@RequestParam String q) { return ResponseEntity.ok(stokService.ara(q)); }

    @GetMapping("/kritik")
    @Operation(summary = "Kritik stoklar", description = "Kritik seviyeye düşen stokları ve önerilen sipariş miktarlarını listeler")
    public ResponseEntity<List<KritikStokDTO>> kritikStoklar(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokService.kritikStoklar(sirketId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre stok getir", description = "Stok ID'sine göre detayları getirir")
    public ResponseEntity<StokDTO> getir(@PathVariable Long id) { return ResponseEntity.ok(stokService.getir(id)); }

    @PostMapping
    @Operation(summary = "Yeni stok oluştur", description = "Yeni bir stok/ürün oluşturur")
    public ResponseEntity<StokDTO> olustur(@RequestBody @jakarta.validation.Valid StokDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(stokService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Stok güncelle", description = "Stok bilgilerini günceller")
    public ResponseEntity<StokDTO> guncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid StokDTO dto) {
        return ResponseEntity.ok(stokService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Stok sil", description = "Stoku siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) { stokService.sil(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/{id}/hareketler")
    @Operation(summary = "Stok hareketlerini getir", description = "Belirli bir stoğa ait hareketleri listeler")
    public ResponseEntity<List<StokHareketDTO>> hareketler(@PathVariable Long id) {
        return ResponseEntity.ok(stokService.hareketler(id));
    }

    @GetMapping("/hareketler/tum")
    @Operation(summary = "Tüm stok hareketlerini getir", description = "Tüm stok hareketlerini listeler")
    public ResponseEntity<List<StokHareketDTO>> tumHareketler() {
        return ResponseEntity.ok(stokService.tumHareketler());
    }

    @PostMapping("/{id}/hareketler")
    @Operation(summary = "Stok hareketi ekle", description = "Stoka yeni bir giriş/çıkış hareketi ekler")
    public ResponseEntity<StokHareketDTO> hareketEkle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid StokHareketDTO dto) {
        dto.setStokId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(stokService.hareketEkle(dto));
    }

    @DeleteMapping("/hareketler/{hareketId}")
    @Operation(summary = "Stok hareketi sil", description = "Stok hareketini siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hareketSil(@PathVariable Long hareketId) {
        stokService.hareketSil(hareketId);
        return ResponseEntity.noContent().build();
    }
}
