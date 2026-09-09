package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.SohbetMesajDTO;
import com.raspel.erp.dto.sistem.SohbetOdaDTO;
import com.raspel.erp.service.sistem.SohbetOdaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Sohbet Odaları", description = "Grup sohbet odaları API")
@RestController
@RequestMapping("/api/sohbet/odalar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class SohbetOdaController {

    private final SohbetOdaService odaService;

    @GetMapping
    @Operation(summary = "Şirketin sohbet odalarını listele")
    public ResponseEntity<List<SohbetOdaDTO>> odalar(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(odaService.odalar(sirketId, kullaniciId));
    }

    @PostMapping
    @Operation(summary = "Yeni sohbet odası oluştur")
    public ResponseEntity<SohbetOdaDTO> olustur(@Valid @RequestBody SohbetOdaDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(odaService.olustur(dto, sirketId, kullaniciId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sohbet odasını güncelle")
    public ResponseEntity<SohbetOdaDTO> guncelle(@PathVariable Long id, @RequestBody SohbetOdaDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(odaService.guncelle(id, dto, sirketId, kullaniciId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Sohbet odasını sil")
    public ResponseEntity<Void> sil(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        odaService.sil(id, sirketId, kullaniciId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/katil")
    @Operation(summary = "Odaya katıl")
    public ResponseEntity<SohbetOdaDTO> katil(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(odaService.katil(id, sirketId, kullaniciId));
    }

    @DeleteMapping("/{id}/katil")
    @Operation(summary = "Odadan ayrıl")
    public ResponseEntity<Void> ayril(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        odaService.ayril(id, sirketId, kullaniciId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/uye")
    @Operation(summary = "Odaya üye ekle")
    public ResponseEntity<SohbetOdaDTO> uyeEkle(@PathVariable Long id, @RequestBody Map<String, Long> body, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        Long hedef = body != null ? body.get("kullaniciId") : null;
        return ResponseEntity.ok(odaService.uyeEkle(id, hedef, sirketId, kullaniciId));
    }

    @DeleteMapping("/{id}/uye/{kullaniciId}")
    @Operation(summary = "Odadan üye çıkar")
    public ResponseEntity<SohbetOdaDTO> uyeCikar(@PathVariable Long id, @PathVariable Long kullaniciId, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long istekSahibiId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(odaService.uyeCikar(id, kullaniciId, sirketId, istekSahibiId));
    }

    @GetMapping("/{id}/mesajlar")
    @Operation(summary = "Odanın son mesajlarını getir")
    public ResponseEntity<List<SohbetMesajDTO>> mesajlar(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(odaService.mesajlar(id, sirketId, kullaniciId));
    }

    @PostMapping("/{id}/mesajlar")
    @Operation(summary = "Odaya mesaj gönder")
    public ResponseEntity<SohbetMesajDTO> mesajGonder(@PathVariable Long id, @Valid @RequestBody SohbetMesajDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String displayName = (String) request.getAttribute("displayName");
        return ResponseEntity.ok(odaService.mesajGonder(id, dto, sirketId, kullaniciId, displayName));
    }

    @PostMapping("/{id}/okundu")
    @Operation(summary = "Odayı okundu işaretle", description = "Odadaki mesajları okundu olarak işaretler")
    public ResponseEntity<Void> okundu(@PathVariable Long id, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        odaService.okunduIsaretle(id, kullaniciId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/dosya")
    @Operation(summary = "Odaya dosya yükle", description = "Sohbette paylaşılmak üzere dosya/görsel yükler")
    public ResponseEntity<Map<String, String>> dosyaYukle(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(Map.of("url", odaService.dosyaYukle(id, file, sirketId, kullaniciId)));
    }
}
