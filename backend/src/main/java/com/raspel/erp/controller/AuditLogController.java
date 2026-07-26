package com.raspel.erp.controller;

import com.raspel.erp.entity.AuditLog;
import com.raspel.erp.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Denetim Log", description = "Denetim log yönetimi API")
@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Denetim loglarını getir", description = "Denetim log kayıtlarını filtreleyerek listeler")
    public ResponseEntity<Page<AuditLog>> tumu(
            @RequestParam(required = false) Long kullaniciId,
            @RequestParam(required = false) String islem,
            @RequestParam(required = false) String entityAdi,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangicTarih,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitisTarih,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.filtreliGetir(kullaniciId, islem, entityAdi, baslangicTarih, bitisTarih, pageable));
    }

    @GetMapping("/islem-tipleri")
    @Operation(summary = "İşlem tiplerini getir", description = "Mevcut işlem tiplerinin listesini döndürür")
    public ResponseEntity<List<String>> islemTipleri() {
        return ResponseEntity.ok(auditLogService.islemTipleri());
    }

    @GetMapping("/entity-listesi")
    @Operation(summary = "Entity listesini getir", description = "Log'u tutulan entity adlarının listesini döndürür")
    public ResponseEntity<List<String>> entityListesi() {
        return ResponseEntity.ok(auditLogService.entityListesi());
    }
}
