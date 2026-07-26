package com.raspel.erp.controller;

import com.raspel.erp.entity.AuditLog;
import com.raspel.erp.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@Tag(name = "Denetim Log", description = "Denetim log yönetimi API")
@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Tüm denetim loglarını getir", description = "Tüm denetim log kayıtlarını listeler")
    public ResponseEntity<Page<AuditLog>> tumu(@PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.tumunuGetir(pageable));
    }
}
