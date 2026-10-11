package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.KitaplikDTO;
import com.raspel.erp.service.sistem.KitaplikService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Kitaplik API: yer imleri (URL) ve snippet'ler (metin) icin ortak uc koseler.
 *
 * <p>Kapsam/gorunurluk sozlesmesi sifre kasasi ile aynidir; GLOBAL kayitlari
 * yalnizca ADMIN yazar. Bu modul sir icermez (sifreleme/no-store gerekmez).
 */
@Tag(name = "Kitaplık", description = "Yer imleri ve snippet kitaplığı API")
@RestController
@RequestMapping("/api/kitaplik")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
public class KitaplikController {

    private final KitaplikService kitaplikService;

    @GetMapping
    @Operation(summary = "Kitaplık listesi", description = "Görünür yer imi/snippet kayıtlarını döner")
    public ResponseEntity<List<KitaplikDTO>> listele(
            @RequestParam(required = false) String tur,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) String kategori,
            @RequestParam(required = false, name = "q") String arama,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        Boolean aktif = durumToAktif(durum);
        return ResponseEntity.ok(kitaplikService.listele(
                sirketId, kullaniciId, request.isUserInRole("ADMIN"), tur, aktif, kategori, arama));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Kayıt detayı", description = "ID'ye göre kitaplık kaydını döner")
    public ResponseEntity<KitaplikDTO> idyeGore(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(kitaplikService.idyeGoreGetir(
                id, sirketId, kullaniciId, request.isUserInRole("ADMIN")));
    }

    @PostMapping
    @Operation(summary = "Yeni kayıt", description = "Kişisel kayıt: herkes. Global kayıt: yalnızca yönetici.")
    public ResponseEntity<KitaplikDTO> olustur(@Valid @RequestBody KitaplikDTO dto,
                                               HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(kitaplikService.olustur(dto, sirketId, kullaniciId, request.isUserInRole("ADMIN")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Kayıt güncelle", description = "Kişisel kaydı sahibi, global kaydı yönetici düzenler")
    public ResponseEntity<KitaplikDTO> guncelle(@PathVariable Long id,
                                                @Valid @RequestBody KitaplikDTO dto,
                                                HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(kitaplikService.guncelle(
                id, dto, sirketId, kullaniciId, request.isUserInRole("ADMIN")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kaydı arşivle", description = "Fiziksel silme yok; kayıt arşive alınır ve geri alınabilir")
    public ResponseEntity<Void> arsivle(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        kitaplikService.arsivle(id, sirketId, kullaniciId, request.isUserInRole("ADMIN"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/geri-al")
    @Operation(summary = "Arşivden geri al", description = "Arşivlenen kaydı yeniden aktifleştirir")
    public ResponseEntity<Void> arsivleGeriAl(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        kitaplikService.arsivleGeriAl(id, sirketId, kullaniciId, request.isUserInRole("ADMIN"));
        return ResponseEntity.noContent().build();
    }

    /** durum: AKTIF (varsayilan) | ARSIV | TUMU -> repository aktif filtresi. */
    private Boolean durumToAktif(String durum) {
        if (durum == null || durum.isBlank() || "AKTIF".equalsIgnoreCase(durum)) return true;
        if ("ARSIV".equalsIgnoreCase(durum)) return false;
        if ("TUMU".equalsIgnoreCase(durum)) return null;
        return true;
    }
}