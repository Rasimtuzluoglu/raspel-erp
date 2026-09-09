package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.AjandaGorevDTO;
import com.raspel.erp.dto.sistem.AjandaHatirlaticiDTO;
import com.raspel.erp.dto.sistem.AjandaOlayDTO;
import com.raspel.erp.service.sistem.AjandaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Ajanda", description = "Ajanda/takvim API")
@RestController
@RequestMapping("/api/ajanda")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class AjandaController {

    private final AjandaService ajandaService;

    @GetMapping
    @Operation(summary = "Ajanda olaylarını getir", description = "Belirtilen tarih aralığındaki görev ve vade olaylarını getirir")
    public ResponseEntity<List<AjandaOlayDTO>> olaylar(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(ajandaService.olaylar(sirketId, baslangic, bitis, kullaniciId));
    }

    // ---------- Kişisel Görevler ----------

    @GetMapping("/tasks")
    @Operation(summary = "Görevleri listele", description = "Oturum açmış kullanıcının kişisel görevlerini listeler")
    public ResponseEntity<List<AjandaGorevDTO>> gorevler(HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(ajandaService.gorevler(kullaniciId));
    }

    @PostMapping("/tasks")
    @Operation(summary = "Görev oluştur", description = "Yeni kişisel görev oluşturur")
    public ResponseEntity<AjandaGorevDTO> gorevOlustur(@Valid @RequestBody AjandaGorevDTO dto, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(ajandaService.gorevOlustur(dto, kullaniciId, sirketId));
    }

    @PutMapping("/tasks/{id}")
    @Operation(summary = "Görev güncelle", description = "Kişisel görevi günceller")
    public ResponseEntity<AjandaGorevDTO> gorevGuncelle(@PathVariable Long id, @RequestBody AjandaGorevDTO dto, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(ajandaService.gorevGuncelle(id, dto, kullaniciId));
    }

    @PostMapping("/tasks/{id}/complete")
    @Operation(summary = "Görevi tamamla", description = "Kişisel görevi tamamlandı olarak işaretler")
    public ResponseEntity<AjandaGorevDTO> gorevTamamla(@PathVariable Long id, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(ajandaService.gorevTamamla(id, kullaniciId));
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "Görev sil", description = "Kişisel görevi siler")
    public ResponseEntity<Void> gorevSil(@PathVariable Long id, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        ajandaService.gorevSil(id, kullaniciId);
        return ResponseEntity.noContent().build();
    }

    // ---------- Kişisel Hatırlatıcılar ----------

    @GetMapping("/reminders")
    @Operation(summary = "Hatırlatıcıları listele", description = "Oturum açmış kullanıcının kişisel hatırlatıcılarını listeler")
    public ResponseEntity<List<AjandaHatirlaticiDTO>> hatirlaticilar(HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(ajandaService.hatirlaticilar(kullaniciId));
    }

    @PostMapping("/reminders")
    @Operation(summary = "Hatırlatıcı oluştur", description = "Yeni kişisel hatırlatıcı oluşturur")
    public ResponseEntity<AjandaHatirlaticiDTO> hatirlaticiOlustur(@RequestBody AjandaHatirlaticiDTO dto, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(ajandaService.hatirlaticiOlustur(dto, kullaniciId, sirketId));
    }

    @DeleteMapping("/reminders/{id}")
    @Operation(summary = "Hatırlatıcı sil", description = "Kişisel hatırlatıcıyı siler")
    public ResponseEntity<Void> hatirlaticiSil(@PathVariable Long id, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        ajandaService.hatirlaticiSil(id, kullaniciId);
        return ResponseEntity.noContent().build();
    }
}
