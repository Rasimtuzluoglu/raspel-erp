package com.raspel.erp.controller.ik;

import com.raspel.erp.dto.ik.PersonelDTO;
import com.raspel.erp.service.ik.PersonelService;
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
import org.springframework.data.web.PageableDefault;
import java.util.List;
import com.raspel.erp.entity.ik.Personel;

@Tag(name = "Personel", description = "Personel yönetimi API")
@RestController
@RequestMapping("/api/personel")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class PersonelController {

    private final PersonelService personelService;

    @GetMapping
    @Operation(summary = "Tüm personeli getir (sayfalı)", description = "Tüm personel kayıtlarını sayfalı olarak listeler")
    public ResponseEntity<Page<PersonelDTO>> tumu(
            jakarta.servlet.http.HttpServletRequest request,
            @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(personelService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/ozet")
    @Operation(summary = "Personel KPI özeti",
            description = "Toplam/aktif/pasif personel sayıları ve onay bekleyen izin talebi sayısı")
    public ResponseEntity<com.raspel.erp.dto.ik.PersonelOzetDTO> ozet(jakarta.servlet.http.HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(personelService.ozet(sirketId));
    }

    @GetMapping("/filtreli")
    @Operation(summary = "Personel ara/filtrele (sayfalı)",
            description = "Ad/soyad/departman/pozisyon/telefon/e-posta araması; departman, rol ve durum filtreleri")
    public ResponseEntity<Page<PersonelDTO>> filtreli(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String departman,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String durum,
            @PageableDefault(size = 50, sort = "ad") Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(personelService.filtreli(sirketId, q, departman, rol, durum, pageable));
    }

    @GetMapping("/departmanlar")
    @Operation(summary = "Departman listesi", description = "Filtre seçenekleri için şirketteki farklı departmanları listeler")
    public ResponseEntity<List<String>> departmanlar(jakarta.servlet.http.HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(personelService.departmanlar(sirketId));
    }

    @GetMapping("/rol/{rol}")
    @Operation(summary = "Göreve göre personel", description = "Belirtilen görevdeki (SOFOR/DEPOCU/DIGER) personelleri listeler")
    public ResponseEntity<java.util.List<PersonelDTO>> roleGore(
            @PathVariable String rol, jakarta.servlet.http.HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(personelService.roleGoreGetir(sirketId, rol));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre personel getir", description = "Personel ID'sine göre detayları getirir")
    public ResponseEntity<PersonelDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(personelService.getir(id));
    }

    @GetMapping("/{id}/detay")
    @Operation(summary = "Personel detayı",
            description = "Özlük bilgileri, son izinler ve son 30 gün puantaj özeti (maaş yalnızca ADMIN/MUHASEBE)")
    public ResponseEntity<com.raspel.erp.dto.ik.PersonelDetayDTO> detay(@PathVariable Long id) {
        return ResponseEntity.ok(personelService.detay(id));
    }

    @PostMapping
    @Operation(summary = "Yeni personel oluştur", description = "Yeni bir personel kaydı oluşturur (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PersonelDTO> olustur(@Valid @RequestBody PersonelDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personelService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Personel güncelle", description = "Personel bilgilerini günceller (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PersonelDTO> guncelle(@PathVariable Long id, @Valid @RequestBody PersonelDTO dto) {
        return ResponseEntity.ok(personelService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Personel sil", description = "Personel kaydını siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        personelService.sil(id);
        return ResponseEntity.noContent().build();
    }
}