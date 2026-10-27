package com.raspel.erp.controller.muhasebe;

import com.raspel.erp.dto.muhasebe.IrsaliyeDTO;
import com.raspel.erp.service.muhasebe.IrsaliyeService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.util.Map;
import java.util.List;

@Tag(name = "İrsaliyeler", description = "İrsaliye yönetimi API")
@RestController
@RequestMapping("/api/irsaliyeler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class IrsaliyeController {

    private final IrsaliyeService irsaliyeService;

    @GetMapping
    @Operation(summary = "Tüm irsaliyeleri getir", description = "Tüm irsaliyeleri listeler")
    public ResponseEntity<Page<IrsaliyeDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(irsaliyeService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre irsaliye getir", description = "İrsaliye ID'sine göre detayları getirir")
    public ResponseEntity<IrsaliyeDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(irsaliyeService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni irsaliye oluştur", description = "Yeni bir irsaliye oluşturur")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IRSALIYE_WRITE')")
    public ResponseEntity<IrsaliyeDTO> olustur(HttpServletRequest request, @Valid @RequestBody IrsaliyeDTO dto) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(irsaliyeService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "İrsaliye güncelle", description = "İrsaliye bilgilerini günceller")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IRSALIYE_WRITE')")
    public ResponseEntity<IrsaliyeDTO> guncelle(@PathVariable Long id, @Valid @RequestBody IrsaliyeDTO dto) {
        return ResponseEntity.ok(irsaliyeService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    @Operation(summary = "İrsaliye durum güncelle", description = "İrsaliye durumunu günceller")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IRSALIYE_WRITE')")
    public ResponseEntity<IrsaliyeDTO> durumGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid com.raspel.erp.dto.sistem.DurumGuncelleRequest body) {
        return ResponseEntity.ok(irsaliyeService.durumGuncelle(id, body.getDurum()));
    }

    @PostMapping("/{id}/hazirla")
    @Operation(summary = "İrsaliyeyi hazırla (TASLAK -> HAZIRLANDI)")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IRSALIYE_WRITE')")
    public ResponseEntity<IrsaliyeDTO> hazirla(@PathVariable Long id, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(irsaliyeService.hazirla(id, kullaniciId));
    }

    @PostMapping("/{id}/sevk")
    @Operation(summary = "İrsaliyeyi sevk et (HAZIRLANDI -> SEVK_EDILDI)")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IRSALIYE_WRITE')")
    public ResponseEntity<IrsaliyeDTO> sevk(
            @PathVariable Long id,
            @RequestBody(required = false) com.raspel.erp.dto.muhasebe.IrsaliyeSevkIstek istek,
            HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(irsaliyeService.sevk(id, istek, kullaniciId));
    }

    @PostMapping("/{id}/teslim")
    @Operation(summary = "İrsaliyeyi teslim et (SEVK_EDILDI -> TESLIM_EDILDI)")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IRSALIYE_WRITE')")
    public ResponseEntity<IrsaliyeDTO> teslim(
            @PathVariable Long id,
            @RequestBody(required = false) com.raspel.erp.dto.muhasebe.IrsaliyeTeslimIstek istek,
            HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(irsaliyeService.teslim(id, istek, kullaniciId));
    }

    @PostMapping("/{id}/sevki-geri-al")
    @Operation(summary = "Sevki geri al (SEVK_EDILDI -> HAZIRLANDI)",
            description = "Stok iadesi yapar ve otomatik teslimatı iptal eder. Canlı fatura varsa reddedilir.")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IRSALIYE_WRITE')")
    public ResponseEntity<IrsaliyeDTO> sevkiGeriAl(@PathVariable Long id, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(irsaliyeService.sevkiGeriAl(id, kullaniciId));
    }

    @PostMapping("/{id}/iptal")
    @Operation(summary = "İrsaliyeyi iptal et",
            description = "Sevk edilmiş/teslim edilmiş kayıtta stok iadesi yapar. Canlı fatura varsa reddedilir.")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IRSALIYE_WRITE')")
    public ResponseEntity<IrsaliyeDTO> iptal(
            @PathVariable Long id,
            @RequestBody(required = false) com.raspel.erp.dto.muhasebe.IrsaliyeIptalIstek istek,
            HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String aciklama = istek != null ? istek.getAciklama() : null;
        return ResponseEntity.ok(irsaliyeService.iptal(id, aciklama, kullaniciId));
    }

    @GetMapping("/filtreli")
    @Operation(summary = "Filtreli irsaliye listesi",
            description = "Arama, durum, tür, cari, tarih aralığı ve faturalı/faturasız filtreleriyle sayfalı liste")
    public ResponseEntity<Page<IrsaliyeDTO>> filtreli(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) String tur,
            @RequestParam(required = false) Long cariId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate baslangic,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bitis,
            @RequestParam(required = false) Boolean faturali,
            @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(irsaliyeService.filtreli(sirketId, q, durum, tur, cariId,
                baslangic, bitis, faturali, pageable));
    }

    @GetMapping("/ozet")
    @Operation(summary = "İrsaliye KPI özeti",
            description = "Taslak, hazırlanan, sevk edilen, teslim edilen (bu ay), faturasız ve iptal sayıları")
    public ResponseEntity<com.raspel.erp.dto.muhasebe.IrsaliyeOzetDTO> ozet(
            HttpServletRequest request,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate baslangic,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(irsaliyeService.ozet(sirketId, baslangic, bitis));
    }

    @PostMapping("/toplu-fatura")
    @Operation(summary = "Toplu faturaya dönüştür",
            description = "Aynı cariye ait birden çok irsaliyeyi tek istekte faturalar; kayıt bazında sonuç döner.")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IRSALIYE_WRITE')")
    public ResponseEntity<com.raspel.erp.dto.muhasebe.TopluFaturaSonucDTO> topluFatura(
            @RequestBody com.raspel.erp.dto.muhasebe.TopluFaturaIstek istek,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(irsaliyeService.topluFatura(istek != null ? istek.getIds() : null,
                sirketId, kullaniciId));
    }

    @GetMapping("/{id}/gecmis")
    @Operation(summary = "İrsaliye durum/işlem geçmişi")
    public ResponseEntity<List<com.raspel.erp.dto.muhasebe.IrsaliyeDurumLogDTO>> gecmis(@PathVariable Long id) {
        return ResponseEntity.ok(irsaliyeService.gecmis(id));
    }

    @PostMapping("/{id}/faturaya-donustur")
    @Operation(summary = "İrsaliyeyi faturaya dönüştür",
            description = "Kesilmiş irsaliyeyi faturaya çevirir; stok tekrar düşülmez (irsaliyeId bağı).")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IRSALIYE_WRITE')")
    public ResponseEntity<IrsaliyeDTO> faturayaDonustur(@PathVariable Long id,
                                                        jakarta.servlet.http.HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(irsaliyeService.faturayaDonustur(id, sirketId, kullaniciId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "İrsaliye sil", description = "İrsaliyeyi siler")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IRSALIYE_DELETE')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        irsaliyeService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
