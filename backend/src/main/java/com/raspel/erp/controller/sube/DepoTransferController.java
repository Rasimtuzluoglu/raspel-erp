package com.raspel.erp.controller.sube;

import com.raspel.erp.dto.sube.DepoTransferDTO;
import com.raspel.erp.service.sube.DepoTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Depo Transferleri", description = "Depolar arası stok transfer onay akışı API")
@RestController
@RequestMapping("/api/depo-transferler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class DepoTransferController {

    private final DepoTransferService depoTransferService;

    @GetMapping
    @Operation(summary = "Transferleri listele", description = "Tüm depo transferlerini listeler")
    public ResponseEntity<List<DepoTransferDTO>> listele(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(depoTransferService.listele(sirketId));
    }

    @GetMapping("/bekleyenler")
    @Operation(summary = "Bekleyen transferleri listele", description = "Onay bekleyen depo transferlerini listeler")
    public ResponseEntity<List<DepoTransferDTO>> bekleyenler(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(depoTransferService.bekleyenler(sirketId));
    }

    @GetMapping("/sayfali")
    @Operation(summary = "Transferleri filtreli/sayfalı listele",
            description = "Depo, durum ve tarih aralığı ile sayfalama")
    public ResponseEntity<Page<DepoTransferDTO>> sayfali(
            @RequestParam(required = false) Long depoId,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate bitis,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(depoTransferService.sayfali(sirketId, depoId, durum, baslangic, bitis, pageable));
    }

    @PostMapping
    @Operation(summary = "Transfer talebi oluştur", description = "Onay bekleyen depo transferi talebi oluşturur")
    public ResponseEntity<DepoTransferDTO> talepOlustur(@jakarta.validation.Valid @RequestBody DepoTransferDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.status(HttpStatus.CREATED).body(depoTransferService.talepOlustur(dto, sirketId, kullaniciId));
    }

    @PostMapping("/{id}/onayla")
    @Operation(summary = "Transferi onayla", description = "Bekleyen depo transferini onaylar ve stok hareketini gerçekleştirir")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepoTransferDTO> onayla(@PathVariable Long id, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(depoTransferService.onayla(id, kullaniciId));
    }

    @PostMapping("/{id}/reddet")
    @Operation(summary = "Transferi reddet", description = "Bekleyen depo transferini gerekçeyle reddeder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepoTransferDTO> reddet(@PathVariable Long id,
                                                  @RequestBody(required = false) Map<String, String> body,
                                                  HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String sebep = body != null ? body.get("sebep") : null;
        return ResponseEntity.ok(depoTransferService.reddet(id, kullaniciId, sebep));
    }
}
