package com.raspel.erp.controller.sistem;

import com.raspel.erp.entity.sistem.Rol;
import com.raspel.erp.entity.sistem.Yetki;
import com.raspel.erp.service.sistem.YetkiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Tag(name = "Rol & Yetki Matrisi", description = "Rol ve detaylı modül yetki yönetimi API")
@RestController
@RequestMapping("/api/yetkiler")
@RequiredArgsConstructor
public class YetkiController {

    private final YetkiService yetkiService;

    @GetMapping
    @Operation(summary = "Tüm yetkileri getir", description = "Sistemdeki tüm modül ve eylem yetkilerini listeler")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Yetki>> tumYetkiler() {
        return ResponseEntity.ok(yetkiService.tumYetkileriGetir());
    }

    @GetMapping("/roller")
    @Operation(summary = "Tüm rolleri getir", description = "Tüm tanımlı rolleri ve yetkilerini listeler")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Rol>> tumRoller() {
        return ResponseEntity.ok(yetkiService.tumRolleriGetir());
    }

    @PutMapping("/roller/{rolId}")
    @Operation(summary = "Rol yetkilerini güncelle", description = "Belirtilen role ait yetki id kümesini günceller")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Rol> rolYetkiGuncelle(@PathVariable Long rolId, @RequestBody Set<Long> yetkiIdleri) {
        return ResponseEntity.ok(yetkiService.rolYetkileriniGuncelle(rolId, yetkiIdleri));
    }
}
