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
    private final com.raspel.erp.service.sistem.AnomaliDurumService anomaliDurumService;

    @GetMapping
    @Operation(summary = "Anomalileri tara ve listele", description = "Sistemdeki mükerrer kayıt ve finansal anomalileri tarar")
    public ResponseEntity<List<AnomaliDTO>> anomalileriGetir(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        List<AnomaliDTO> list = anomaliTespitEngine.anomalileriTara(sirketId);
        anomaliDurumService.uygula(sirketId, list);
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{anahtar}/durum")
    @Operation(summary = "Anomali inceleme durumunu güncelle",
            description = "INCELENDI / YOK_SAYILDI işaretler; ACIK gönderilirse işaret kaldırılır")
    public ResponseEntity<Void> durumGuncelle(
            @PathVariable String anahtar,
            @RequestBody java.util.Map<String, Object> body,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String durum = body != null && body.get("durum") != null ? String.valueOf(body.get("durum")) : null;
        anomaliDurumService.isaretle(sirketId, anahtar, durum, kullaniciId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ip-whitelist")
    @Operation(summary = "IP Beyaz Listesini getir", description = "Şirkete tanımlı güvenli IP adreslerini listeler")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<java.util.Map<String, Object>>> getIpWhitelist(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(ipWhitelistService.listele(sirketId));
    }

    @PostMapping("/ip-whitelist")
    @Operation(summary = "IP Beyaz Listesine IP ekle", description = "Yeni bir güvenli IP adresi tanımlar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<java.util.Map<String, Object>>> addIpWhitelist(
            @RequestBody java.util.Map<String, Object> entry, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(ipWhitelistService.ekle(sirketId, entry));
    }

    @PutMapping("/ip-whitelist/{id}")
    @Operation(summary = "IP Beyaz Listesi kaydını güncelle", description = "IP/CIDR, açıklama ve durumu günceller")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<java.util.Map<String, Object>>> updateIpWhitelist(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> entry,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(ipWhitelistService.guncelle(sirketId, id, entry));
    }

    @DeleteMapping("/ip-whitelist/{id}")
    @Operation(summary = "IP Beyaz Listesinden IP sil", description = "Tanımlı IP adresini listeden kaldırır")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<java.util.Map<String, Object>>> deleteIpWhitelist(
            @PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(ipWhitelistService.sil(sirketId, id));
    }
}