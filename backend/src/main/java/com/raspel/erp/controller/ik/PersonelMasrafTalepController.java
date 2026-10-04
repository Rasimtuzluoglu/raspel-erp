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
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'DRIVER')")
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
            @jakarta.validation.Valid @RequestBody PersonelMasrafTalepDTO dto,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.status(HttpStatus.CREATED).body(talepService.talepOlustur(dto, sirketId, kullaniciId));
    }

    @PatchMapping("/{id}/onayla")
    @Operation(summary = "Talebi onayla ve finans masraflarına aktar")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<PersonelMasrafTalepDTO> onayla(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        String onaylayan = onaylayanKisi(request);
        String not = (body != null) ? body.get("onayNotu") : null;
        return ResponseEntity.ok(talepService.onayla(id, onaylayan, not));
    }

    @PatchMapping("/{id}/reddet")
    @Operation(summary = "Talebi reddet")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<PersonelMasrafTalepDTO> reddet(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        String onaylayan = onaylayanKisi(request);
        String not = (body != null) ? body.get("onayNotu") : null;
        return ResponseEntity.ok(talepService.reddet(id, onaylayan, not));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Talebi sil")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        talepService.sil(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * REDTEAM/Faz1.3: Onaylayan kimligi DAIMA dogrulanmis SecurityContext'ten
     * alinir; istemci govdesinden ASLA alinmaz.
     *
     * <p>Eskiden {@code request.getAttribute("username")} okunuyordu ve bu
     * nitelik JwtAuthFilter'da hic set edilmedigi icin her zaman {@code null}
     * geliyor, kayit "Yonetici" yaziyordu — denetim izi hicbir kisiyi
     * tanimlamiyordu. Ayrica istemcinin gonderdigi {@code onaylayan} alani
     * kullanilsaydi sahte onay imzasi yazilabilirdi.
     *
     * @return onaylayan kullanici adi
     */
    private String onaylayanKisi(HttpServletRequest request) {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
            return auth.getName();
        }
        // JwtAuthFilter iki yolda da username set ediyor; bu yalnizca beklenmeyen
        // durum (SecurityContext bos) icin guvenli yedektir.
        Object attr = (request != null) ? request.getAttribute("username") : null;
        return (attr instanceof String s && !s.isBlank()) ? s : "Bilinmiyor";
    }
}
