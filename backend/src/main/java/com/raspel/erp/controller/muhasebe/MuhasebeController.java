package com.raspel.erp.controller.muhasebe;

import com.raspel.erp.dto.muhasebe.*;
import com.raspel.erp.service.muhasebe.MuhasebeService;
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
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.muhasebe.HesapPlani;

@Tag(name = "Genel Muhasebe", description = "Hesap planı, yevmiye, mizan ve defteri kebir API")
@RestController
@RequestMapping("/api/muhasebe")
@RequiredArgsConstructor
public class MuhasebeController {

    private final MuhasebeService muhasebeService;

    private Long sirketId(HttpServletRequest request) {
        return (Long) request.getAttribute("sirketId");
    }

    // HESAP PLANI

    @GetMapping("/hesap-plani")
    @Operation(summary = "Hesap planını getir", description = "Şirketin hesap planını listeler (boşsa varsayılan plan oluşturur)")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<HesapPlaniDTO>> hesapPlani(HttpServletRequest request) {
        return ResponseEntity.ok(muhasebeService.hesapPlaniniGetir(sirketId(request)));
    }

    @PostMapping("/hesap-plani")
    @Operation(summary = "Yeni hesap ekle", description = "Hesap planına yeni hesap ekler")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HesapPlaniDTO> hesapOlustur(@Valid @RequestBody HesapPlaniDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(muhasebeService.hesapOlustur(dto));
    }

    @PutMapping("/hesap-plani/{id}")
    @Operation(summary = "Hesap güncelle", description = "Hesap planındaki hesabı günceller")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HesapPlaniDTO> hesapGuncelle(@PathVariable Long id, @Valid @RequestBody HesapPlaniDTO dto) {
        return ResponseEntity.ok(muhasebeService.hesapGuncelle(id, dto));
    }

    @DeleteMapping("/hesap-plani/{id}")
    @Operation(summary = "Hesap sil", description = "Hesap planındaki hesabı siler")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hesapSil(@PathVariable Long id) {
        muhasebeService.hesapSil(id);
        return ResponseEntity.noContent().build();
    }

    // YEVMIYE FİŞLERİ

    @GetMapping("/fisler")
    @Operation(summary = "Yevmiye fişlerini getir", description = "Tarih aralığına göre muhasebe fişlerini listeler")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<MuhasebeFisiDTO>> fisler(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return ResponseEntity.ok(muhasebeService.fisleriGetir(sirketId(request), baslangic, bitis));
    }

    @GetMapping("/fisler/{id}")
    @Operation(summary = "Fiş detayını getir", description = "Muhasebe fişini kalemleriyle birlikte getirir")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<MuhasebeFisiDTO> fisGetir(@PathVariable Long id) {
        return ResponseEntity.ok(muhasebeService.fisGetir(id));
    }

    @PostMapping("/fisler")
    @Operation(summary = "Yeni fiş oluştur", description = "Borç/alacak dengeli muhasebe fişi (yevmiye kaydı) oluşturur")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<MuhasebeFisiDTO> fisOlustur(@Valid @RequestBody MuhasebeFisiDTO dto, HttpServletRequest request) {
        if (dto.getSirketId() == null) dto.setSirketId(sirketId(request));
        dto.setKullaniciId((Long) request.getAttribute("kullaniciId"));
        return ResponseEntity.status(HttpStatus.CREATED).body(muhasebeService.fisOlustur(dto));
    }

    @PostMapping("/fisler/{id}/iptal")
    @Operation(summary = "Fişi iptal et", description = "Kayıtlı fişi iptal eder")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> fisIptal(@PathVariable Long id) {
        muhasebeService.fisIptalEt(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/fisler/{id}")
    @Operation(summary = "Fişi sil", description = "Muhasebe fişini siler")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> fisSil(@PathVariable Long id) {
        muhasebeService.fisSil(id);
        return ResponseEntity.noContent().build();
    }

    // MİZAN

    @GetMapping("/mizan")
    @Operation(summary = "Mizan getir", description = "Hesap bazında borç/alacak ve bakiye mizanını getirir")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<MizanSatiriDTO>> mizan(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return ResponseEntity.ok(muhasebeService.mizanGetir(sirketId(request), baslangic, bitis));
    }

    // DEFTER-İ KEBİR

    @GetMapping("/defteri-kebir")
    @Operation(summary = "Defteri kebir getir", description = "Hesap bazında yevmiye hareket dökümü getirir")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DefteriKebirSatiriDTO>> defteriKebir(
            HttpServletRequest request,
            @RequestParam(required = false) String hesapKodu,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return ResponseEntity.ok(muhasebeService.defteriKebirGetir(sirketId(request), hesapKodu, baslangic, bitis));
    }
}