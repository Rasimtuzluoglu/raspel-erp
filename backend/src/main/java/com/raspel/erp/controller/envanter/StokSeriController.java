package com.raspel.erp.controller.envanter;

import com.raspel.erp.dto.envanter.StokSeriDTO;
import com.raspel.erp.service.envanter.StokSeriService;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
import com.raspel.erp.entity.envanter.Stok;

@Tag(name = "Stok Seri/Lot", description = "Stok seri ve lot numarası yönetimi API")
@RestController
@RequestMapping("/api/stok-seri")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class StokSeriController {

    private final StokSeriService stokSeriService;

    @GetMapping
    @Operation(summary = "Tüm seri/lotları getir", description = "Tüm stok seri/lot kayıtlarını listeler")
    public ResponseEntity<Page<StokSeriDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokSeriService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/ozet")
    @Operation(summary = "Seri/lot KPI özeti",
            description = "Toplam/stokta/tüketildi, SKT yaklaşan ve geçmiş seri adetleri")
    public ResponseEntity<com.raspel.erp.dto.envanter.StokSeriOzetDTO> ozet(
            HttpServletRequest request,
            @RequestParam(defaultValue = "30") int gun) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokSeriService.ozet(sirketId, gun));
    }

    @GetMapping("/filtreli")
    @Operation(summary = "Seri/lotları filtreli ve sayfalı listele",
            description = "q (ürün/seri/lot), durum, depo ve SKT aralığı destekler")
    public ResponseEntity<Page<StokSeriDTO>> filtreli(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) Long depoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sktBaslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sktBitis,
            @PageableDefault(size = 25, sort = "olusturmaTarihi", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokSeriService.filtreliGetir(
                sirketId, q, durum, depoId, sktBaslangic, sktBitis, pageable));
    }

    @GetMapping("/stok/{stokId}")
    @Operation(summary = "Stoğa göre seri/lot getir", description = "Belirli bir stoğa ait seri/lot numaralarını listeler")
    public ResponseEntity<List<StokSeriDTO>> stokIcinGetir(@PathVariable Long stokId) {
        return ResponseEntity.ok(stokSeriService.stokIcinGetir(stokId));
    }

    @GetMapping("/son-kullanma")
    @Operation(summary = "Son kullanma yaklaşan seri/lotlar", description = "Son kullanma tarihi yaklaşan/geçmiş seri/lotları listeler")
    public ResponseEntity<List<StokSeriDTO>> sonKullanma(HttpServletRequest request, @RequestParam(defaultValue = "30") int gun) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokSeriService.sonKullanmaYaklasan(sirketId, gun));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre seri/lot getir", description = "Seri/lot ID'sine göre detayları getirir")
    public ResponseEntity<StokSeriDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(stokSeriService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni seri/lot oluştur", description = "Yeni bir stok seri/lot kaydı oluşturur")
    public ResponseEntity<StokSeriDTO> olustur(@Valid @RequestBody StokSeriDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stokSeriService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Seri/lot güncelle", description = "Seri/lot kaydını günceller")
    public ResponseEntity<StokSeriDTO> guncelle(@PathVariable Long id, @Valid @RequestBody StokSeriDTO dto) {
        return ResponseEntity.ok(stokSeriService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Seri/lot sil", description = "Seri/lot kaydını siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        stokSeriService.sil(id);
        return ResponseEntity.noContent().build();
    }
}