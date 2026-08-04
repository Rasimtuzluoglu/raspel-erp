package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.GorevDTO;
import com.raspel.erp.dto.sistem.ProjeDTO;
import com.raspel.erp.service.sistem.ProjeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.util.Map;
import com.raspel.erp.entity.sistem.Gorev;
import com.raspel.erp.entity.sistem.Proje;

@Tag(name = "Projeler", description = "Proje yönetimi API")
@RestController
@RequestMapping("/api/projeler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class ProjeController {

    private final ProjeService projeService;

    @GetMapping
    @Operation(summary = "Tüm projeleri getir", description = "Tüm projeleri listeler")
    public ResponseEntity<Page<ProjeDTO>> tumu(@RequestParam(required = false) Long sirketId, @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(projeService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre proje getir", description = "Proje ID'sine göre detayları getirir")
    public ResponseEntity<ProjeDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(projeService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni proje oluştur", description = "Yeni bir proje oluşturur")
    public ResponseEntity<ProjeDTO> olustur(@Valid @RequestBody ProjeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projeService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Proje güncelle", description = "Proje bilgilerini günceller")
    public ResponseEntity<ProjeDTO> guncelle(@PathVariable Long id, @Valid @RequestBody ProjeDTO dto) {
        return ResponseEntity.ok(projeService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    @Operation(summary = "Proje durum güncelle", description = "Proje durumunu günceller")
    public ResponseEntity<ProjeDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(projeService.durumGuncelle(id, body.get("durum")));
    }

    @PutMapping("/gorev/{gorevId}/durum")
    @Operation(summary = "Görev durum güncelle", description = "Proje görevinin durumunu günceller")
    public ResponseEntity<GorevDTO> gorevDurumGuncelle(@PathVariable Long gorevId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(projeService.gorevDurumGuncelle(gorevId, body.get("durum")));
    }

    @PostMapping("/{projeId}/gorevler")
    @Operation(summary = "Projeye görev ekle", description = "Bir projeye yeni bir görev ekler")
    public ResponseEntity<ProjeDTO> gorevEkle(@PathVariable Long projeId, @Valid @RequestBody GorevDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projeService.gorevEkle(projeId, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Proje sil", description = "Projeyi siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        projeService.sil(id);
        return ResponseEntity.noContent().build();
    }
}