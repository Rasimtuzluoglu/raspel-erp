package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.DonemDTO;
import com.raspel.erp.service.sistem.DonemService;
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
import java.util.List;
import com.raspel.erp.entity.sistem.Sirket;

@Tag(name = "Dönemler", description = "Dönem yönetimi API")
@RestController
@RequestMapping("/api/donemler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class DonemController {

    private final DonemService donemService;

    @GetMapping
    @Operation(summary = "Tüm dönemleri getir", description = "Tüm dönemleri listeler")
    public ResponseEntity<Page<DonemDTO>> tumu(jakarta.servlet.http.HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(donemService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/sirket/{sirketId}")
    @Operation(summary = "Şirkete göre dönemleri getir", description = "Belirli bir şirkete ait dönemleri listeler")
    public ResponseEntity<List<DonemDTO>> sirketeGore(@PathVariable Long sirketId, jakarta.servlet.http.HttpServletRequest request) {
        Long aktifSirketId = (Long) request.getAttribute("sirketId");
        if (aktifSirketId != null && !aktifSirketId.equals(sirketId)) {
            throw new com.raspel.erp.exception.ResourceNotFoundException("Donem bu sirkete ait degil");
        }
        return ResponseEntity.ok(donemService.sirketeGoreGetir(sirketId));
    }

    @GetMapping("/sirket/{sirketId}/aktif")
    @Operation(summary = "Aktif dönemleri getir", description = "Şirketin aktif dönemlerini listeler")
    public ResponseEntity<List<DonemDTO>> aktifDonemler(@PathVariable Long sirketId, jakarta.servlet.http.HttpServletRequest request) {
        Long aktifSirketId = (Long) request.getAttribute("sirketId");
        if (aktifSirketId != null && !aktifSirketId.equals(sirketId)) {
            throw new com.raspel.erp.exception.ResourceNotFoundException("Donem bu sirkete ait degil");
        }
        return ResponseEntity.ok(donemService.aktifDonemler(sirketId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre dönem getir", description = "Dönem ID'sine göre detayları getirir")
    public ResponseEntity<DonemDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(donemService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni dönem oluştur", description = "Yeni bir dönem oluşturur (aktif ise diğerleri pasifleşir)")
    public ResponseEntity<DonemDTO> olustur(@Valid @RequestBody DonemDTO dto, jakarta.servlet.http.HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        if (sirketId != null) dto.setSirketId(sirketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(donemService.olustur(dto));
    }

    @PutMapping("/{id}/aktif")
    @Operation(summary = "Dönemi aktif yap", description = "Dönemi aktif yapar ve aynı şirketteki diğer dönemleri pasifleştirir")
    public ResponseEntity<DonemDTO> aktifYap(@PathVariable Long id) {
        return ResponseEntity.ok(donemService.aktifYap(id));
    }

    @PutMapping("/{id}/kilitle")
    @Operation(summary = "Dönemi kilitle", description = "Dönemi kilitler; kilitli dönemdeki belgeler değiştirilemez")
    public ResponseEntity<DonemDTO> kilitle(@PathVariable Long id) {
        return ResponseEntity.ok(donemService.kilitle(id));
    }

    @PutMapping("/{id}/kilit-ac")
    @Operation(summary = "Dönem kilidini aç", description = "Dönemin kilidini açar (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DonemDTO> kilidiAc(@PathVariable Long id) {
        return ResponseEntity.ok(donemService.kilidiAc(id));
    }

    @GetMapping("/{id}/kilitli-mi")
    @Operation(summary = "Tarih kilidi kontrolü", description = "Verilen tarihin kilitli döneme denk gelip gelmediğini döndürür")
    public ResponseEntity<java.util.Map<String, Object>> kilitliMi(
            @PathVariable Long id, @RequestParam String tarih) {
        DonemDTO d = donemService.getir(id);
        boolean kilitli = donemService.tarihKilitliMi(d.getSirketId(), java.time.LocalDate.parse(tarih));
        return ResponseEntity.ok(java.util.Map.of("kilitli", kilitli));
    }

    @PostMapping("/yil-sonu-kapat")
    @Operation(summary = "Yıl sonu kapanışı", description = "Mali yılı kapsayan dönemleri kilitler ve kapanış özetini kaydeder (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.raspel.erp.dto.sistem.DonemKapanisDTO> yilSonuKapat(
            @RequestBody com.raspel.erp.dto.sistem.DonemKapanisDTO dto,
            jakarta.servlet.http.HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        if (sirketId == null) sirketId = dto.getSirketId();
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(donemService.yilSonuKapat(sirketId, dto.getYil(), dto.getOzet(), kullaniciId));
    }

    @GetMapping("/kapanislar")
    @Operation(summary = "Kapanışları getir", description = "Şirketin yıl sonu kapanış kayıtlarını listeler")
    public ResponseEntity<List<com.raspel.erp.dto.sistem.DonemKapanisDTO>> kapanislar(
            jakarta.servlet.http.HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(donemService.kapanislariGetir(sirketId));
    }


    @PutMapping("/{id}")
    @Operation(summary = "Dönem güncelle", description = "Dönem bilgilerini günceller")
    public ResponseEntity<DonemDTO> guncelle(@PathVariable Long id, @Valid @RequestBody DonemDTO dto) {
        return ResponseEntity.ok(donemService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dönem sil", description = "Dönemi siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        donemService.sil(id);
        return ResponseEntity.noContent().build();
    }
}