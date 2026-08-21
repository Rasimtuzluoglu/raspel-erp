package com.raspel.erp.controller.ik;

import com.raspel.erp.dto.ik.PersonelMasrafTalepDTO;
import com.raspel.erp.service.ik.PersonelMasrafTalepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Personel Masraf & Avans Talepleri", description = "Saha personeli harcama ve avans talep yönetimi")
@RestController
@RequestMapping("/api/personel-masraf-talepler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class PersonelMasrafTalepController {

    private final PersonelMasrafTalepService talepService;

    @GetMapping
    @Operation(summary = "Şirketin tüm masraf taleplerini listele")
    public ResponseEntity<Page<PersonelMasrafTalepDTO>> tumunuGetir(
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(talepService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/bekleyenler")
    @Operation(summary = "Onay bekleyen masraf ve avans talepleri")
    public ResponseEntity<List<PersonelMasrafTalepDTO>> bekleyenleriGetir(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(talepService.bekleyenleriGetir(sirketId));
    }

    @GetMapping("/kullanici-talepleri")
    @Operation(summary = "Oturum açmış personelin kendi talepleri")
    public ResponseEntity<List<PersonelMasrafTalepDTO>> kullaniciTalepleri(HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(talepService.kullaniciTalepleri(kullaniciId));
    }

    @PostMapping
    @Operation(summary = "Yeni masraf / avans talebi oluştur")
    public ResponseEntity<PersonelMasrafTalepDTO> talepOlustur(
            @RequestBody PersonelMasrafTalepDTO dto,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.status(HttpStatus.CREATED).body(talepService.talepOlustur(dto, sirketId, kullaniciId));
    }

    @PatchMapping("/{id}/onayla")
    @Operation(summary = "Talebi onayla ve finans masraflarına aktar")
    public ResponseEntity<PersonelMasrafTalepDTO> onayla(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        String onaylayan = (String) request.getAttribute("username");
        String not = (body != null) ? body.get("onayNotu") : null;
        return ResponseEntity.ok(talepService.onayla(id, onaylayan != null ? onaylayan : "Yönetici", not));
    }

    @PatchMapping("/{id}/reddet")
    @Operation(summary = "Talebi reddet")
    public ResponseEntity<PersonelMasrafTalepDTO> reddet(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        String onaylayan = (String) request.getAttribute("username");
        String not = (body != null) ? body.get("onayNotu") : null;
        return ResponseEntity.ok(talepService.reddet(id, onaylayan != null ? onaylayan : "Yönetici", not));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Talebi sil")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        talepService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
