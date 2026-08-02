package com.raspel.erp.controller;

import com.raspel.erp.dto.PersonelIzinDTO;
import com.raspel.erp.service.PersonelIzinService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

@Tag(name = "Personel İzin", description = "Personel izin yönetimi API")
@RestController
@RequestMapping("/api/personel-izin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class PersonelIzinController {

    private final PersonelIzinService personelIzinService;

    @GetMapping
    @Operation(summary = "Tüm izinleri getir", description = "Şirkete ait tüm personel izinlerini listeler")
    public ResponseEntity<Page<PersonelIzinDTO>> tumu(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(personelIzinService.tumunuGetir(sirketId, Pageable.unpaged()));
    }

    @GetMapping("/personel/{personelId}")
    @Operation(summary = "Personele göre izinleri getir", description = "Belirli bir personele ait izinleri listeler")
    public ResponseEntity<List<PersonelIzinDTO>> personelIzinleri(@PathVariable Long personelId) {
        return ResponseEntity.ok(personelIzinService.personelIzınleri(personelId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre izin getir", description = "İzin ID'sine göre detayları getirir")
    public ResponseEntity<PersonelIzinDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(personelIzinService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni izin oluştur", description = "Yeni bir personel izni oluşturur")
    public ResponseEntity<PersonelIzinDTO> olustur(@Valid @RequestBody PersonelIzinDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personelIzinService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "İzin güncelle", description = "İzin bilgilerini günceller")
    public ResponseEntity<PersonelIzinDTO> guncelle(@PathVariable Long id, @Valid @RequestBody PersonelIzinDTO dto) {
        return ResponseEntity.ok(personelIzinService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    @Operation(summary = "İzin durum güncelle", description = "İzin durumunu günceller (onayla/reddet)")
    public ResponseEntity<PersonelIzinDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(personelIzinService.durumGuncelle(id, body.get("durum"), body.get("onaylayan")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "İzin sil", description = "İzin kaydını siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        personelIzinService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
