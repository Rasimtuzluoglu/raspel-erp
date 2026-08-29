package com.raspel.erp.controller.sube;

import com.raspel.erp.dto.sube.DepoTransferDTO;
import com.raspel.erp.service.sube.DepoTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping
    @Operation(summary = "Transfer talebi oluştur", description = "Onay bekleyen depo transferi talebi oluşturur")
    public ResponseEntity<DepoTransferDTO> talepOlustur(@RequestBody DepoTransferDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.status(HttpStatus.CREATED).body(depoTransferService.talepOlustur(dto, sirketId, kullaniciId));
    }

    @PostMapping("/{id}/onayla")
    @Operation(summary = "Transferi onayla", description = "Bekleyen depo transferini onaylar ve stok hareketini gerçekleştirir")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepoTransferDTO> onayla(@PathVariable Long id) {
        return ResponseEntity.ok(depoTransferService.onayla(id));
    }

    @PostMapping("/{id}/reddet")
    @Operation(summary = "Transferi reddet", description = "Bekleyen depo transferini reddeder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepoTransferDTO> reddet(@PathVariable Long id) {
        return ResponseEntity.ok(depoTransferService.reddet(id));
    }
}
