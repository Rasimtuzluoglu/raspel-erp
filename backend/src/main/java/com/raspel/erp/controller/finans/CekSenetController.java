package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.CekSenetDTO;
import com.raspel.erp.service.finans.CekSenetService;
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
import java.util.Map;

@Tag(name = "Çek/Senet", description = "Çek ve senet yönetimi API")
@RestController
@RequestMapping("/api/cek-senet")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class CekSenetController {

    private final CekSenetService cekSenetService;

    @GetMapping
    @Operation(summary = "Tüm çek/senetleri getir", description = "Tüm çek ve senetleri listeler")
    public ResponseEntity<Page<CekSenetDTO>> tumu(jakarta.servlet.http.HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(cekSenetService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre çek/senet getir", description = "Çek/senet ID'sine göre detayları getirir")
    public ResponseEntity<CekSenetDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(cekSenetService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni çek/senet oluştur", description = "Yeni bir çek veya senet oluşturur")
    public ResponseEntity<CekSenetDTO> olustur(@Valid @RequestBody CekSenetDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cekSenetService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Çek/senet güncelle", description = "Çek/senet bilgilerini günceller")
    public ResponseEntity<CekSenetDTO> guncelle(@PathVariable Long id, @Valid @RequestBody CekSenetDTO dto) {
        return ResponseEntity.ok(cekSenetService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    @Operation(summary = "Çek/senet durum güncelle", description = "Çek/senet durumunu günceller (tahsil/tahsil edildi/karşılıksız vb.)")
    public ResponseEntity<CekSenetDTO> durumGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid com.raspel.erp.dto.sistem.DurumGuncelleRequest body) {
        return ResponseEntity.ok(cekSenetService.durumGuncelle(id, body.getDurum()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Çek/senet sil", description = "Çek/senedi siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        cekSenetService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
