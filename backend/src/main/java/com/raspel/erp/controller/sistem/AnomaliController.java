package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.AnomaliDTO;
import com.raspel.erp.service.sistem.AnomaliTespitEngine;
import com.raspel.erp.service.sistem.IpWhitelistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.raspel.erp.entity.ticaret.Fatura;

@Tag(name = "Akıllı Anomali Tespiti", description = "Mükerrer fatura, ödeme ve finansal anomali tespit API")
@RestController
@RequestMapping("/api/anomaliler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class AnomaliController {

    private final AnomaliTespitEngine anomaliTespitEngine;
    private final IpWhitelistService ipWhitelistService;

    @GetMapping
    @Operation(summary = "Anomalileri tara ve listele", description = "Sistemdeki mükerrer kayıt ve finansal anomalileri tarar")
    public ResponseEntity<List<AnomaliDTO>> anomalileriGetir(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(anomaliTespitEngine.anomalileriTara(sirketId));
    }

    @GetMapping("/ip-whitelist")
    @Operation(summary = "IP Beyaz Listesini getir", description = "Şirkete tanımlı güvenli IP adreslerini listeler")
    public ResponseEntity<List<java.util.Map<String, Object>>> getIpWhitelist(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(ipWhitelistService.listele(sirketId));
    }

    @PostMapping("/ip-whitelist")
    @Operation(summary = "IP Beyaz Listesine IP ekle", description = "Yeni bir güvenli IP adresi tanımlar")
    public ResponseEntity<List<java.util.Map<String, Object>>> addIpWhitelist(
            @RequestBody java.util.Map<String, Object> entry, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(ipWhitelistService.ekle(sirketId, entry));
    }

    @DeleteMapping("/ip-whitelist/{id}")
    @Operation(summary = "IP Beyaz Listesinden IP sil", description = "Tanımlı IP adresini listeden kaldırır")
    public ResponseEntity<List<java.util.Map<String, Object>>> deleteIpWhitelist(
            @PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(ipWhitelistService.sil(sirketId, id));
    }
}