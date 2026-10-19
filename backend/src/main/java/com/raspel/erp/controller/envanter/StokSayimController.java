package com.raspel.erp.controller.envanter;

import com.raspel.erp.dto.envanter.StokSayimDTO;
import com.raspel.erp.service.envanter.StokSayimService;
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
import com.raspel.erp.entity.envanter.Stok;

import java.time.LocalDate;

@Tag(name = "Stok Sayım", description = "Stok sayım yönetimi API")
@RestController
@RequestMapping("/api/stok-sayim")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class StokSayimController {

    private final StokSayimService stokSayimService;

    @GetMapping
    @Operation(summary = "Tüm stok sayımlarını getir", description = "Tüm stok sayım kayıtlarını listeler")
    public ResponseEntity<Page<StokSayimDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokSayimService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/ozet")
    @Operation(summary = "Stok sayım KPI özeti",
            description = "Toplam/taslak/tamamlanan/iptal sayıları, farklı ve bugünkü sayım adetleri")
    public ResponseEntity<com.raspel.erp.dto.envanter.StokSayimOzetDTO> ozet(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokSayimService.ozet(sirketId));
    }

    @GetMapping("/filtreli")
    @Operation(summary = "Stok sayımlarını filtreli ve sayfalı listele",
            description = "q (ürün/kod/barkod), durum, tarih aralığı ve sayfalama destekler")
    public ResponseEntity<Page<StokSayimDTO>> filtreli(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @PageableDefault(size = 25, sort = "tarih", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokSayimService.filtreliGetir(sirketId, q, durum, baslangic, bitis, pageable));
    }

    @PostMapping("/tarama")
    @Operation(summary = "Barkod taraması", description = "Barkod ile TASLAK sayımın sayılan miktarını artırır; yoksa yeni sayım başlatır")
    public ResponseEntity<StokSayimDTO> tara(@jakarta.validation.Valid @RequestBody com.raspel.erp.dto.envanter.BarkodTaramaDTO body, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokSayimService.tara(body.getBarkod(), body.getAdet(), sirketId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre stok sayım getir", description = "Stok sayım ID'sine göre detayları getirir")
    public ResponseEntity<StokSayimDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(stokSayimService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni stok sayımı oluştur", description = "Yeni bir stok sayım kaydı oluşturur")
    public ResponseEntity<StokSayimDTO> olustur(@Valid @RequestBody StokSayimDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(stokSayimService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Stok sayım güncelle", description = "Stok sayım kaydını günceller")
    public ResponseEntity<StokSayimDTO> guncelle(@PathVariable Long id, @Valid @RequestBody StokSayimDTO dto) {
        return ResponseEntity.ok(stokSayimService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    @Operation(summary = "Stok sayım durum güncelle", description = "Durumu günceller; TAMAMLANDI olursa stok miktarı sayıma göre düzeltilir")
    public ResponseEntity<StokSayimDTO> durumGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid com.raspel.erp.dto.sistem.DurumGuncelleRequest body) {
        return ResponseEntity.ok(stokSayimService.durumGuncelle(id, body.getDurum()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Stok sayım sil", description = "Stok sayım kaydını siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        stokSayimService.sil(id);
        return ResponseEntity.noContent().build();
    }
}