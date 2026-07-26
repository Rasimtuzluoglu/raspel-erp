package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.DovizKuruDTO;
import com.raspel.erp.service.finans.DovizKuruService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Döviz Kurları", description = "Günlük döviz kuru yönetimi API")
@RestController
@RequestMapping({"/api/doviz-kurlari", "/api/v1/doviz-kurlari"})
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class DovizKuruController {

    private final DovizKuruService dovizKuruService;

    @GetMapping
    @Operation(summary = "Günlük döviz kurlarını getir", description = "Tarihe göre döviz kurlarını listeler (Varsayılan: Bugüne ait kurlar)")
    public ResponseEntity<List<DovizKuruDTO>> gunlukKurlariGetir(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tarih) {
        return ResponseEntity.ok(dovizKuruService.gunlukKurlariGetir(tarih));
    }

    @PostMapping
    @Operation(summary = "Döviz kuru ekle veya güncelle", description = "Belirtilen tarihe ait döviz kurunu kaydeder (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DovizKuruDTO> kurKaydet(@Valid @RequestBody DovizKuruDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dovizKuruService.kurEkleVeyaGuncelle(dto));
    }
}
