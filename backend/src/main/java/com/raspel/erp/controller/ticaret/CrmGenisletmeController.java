package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.CrmAktiviteDTO;
import com.raspel.erp.dto.ticaret.CrmKampanyaDTO;
import com.raspel.erp.dto.ticaret.CrmLeadDTO;
import com.raspel.erp.service.ticaret.CrmGenisletmeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "CRM Genişletme", description = "Lead, aktivite ve kampanya yönetimi API")
@RestController
@RequestMapping("/api/crm")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
public class CrmGenisletmeController {

    private final CrmGenisletmeService crmGenisletmeService;

    // ---------------- Lead ----------------

    @GetMapping("/leadler")
    @Operation(summary = "Lead'leri getir", description = "Şirketin potansiyel müşterilerini listeler")
    public ResponseEntity<Page<CrmLeadDTO>> leadler(HttpServletRequest request,
                                                    @RequestParam(required = false) String durum,
                                                    @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(crmGenisletmeService.leadleriGetir(sirketId, durum, pageable));
    }

    @GetMapping("/leadler/{id}")
    @Operation(summary = "Lead getir", description = "ID'ye göre lead getirir")
    public ResponseEntity<CrmLeadDTO> leadGetir(@PathVariable Long id) {
        return ResponseEntity.ok(crmGenisletmeService.leadGetir(id));
    }

    @PostMapping("/leadler")
    @Operation(summary = "Lead oluştur", description = "Yeni potansiyel müşteri oluşturur")
    public ResponseEntity<CrmLeadDTO> leadOlustur(@Valid @RequestBody CrmLeadDTO dto,
                                                  HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(crmGenisletmeService.leadOlustur(dto, sirketId));
    }

    @PutMapping("/leadler/{id}")
    @Operation(summary = "Lead güncelle", description = "Lead bilgilerini günceller")
    public ResponseEntity<CrmLeadDTO> leadGuncelle(@PathVariable Long id, @Valid @RequestBody CrmLeadDTO dto) {
        return ResponseEntity.ok(crmGenisletmeService.leadGuncelle(id, dto));
    }

    @PutMapping("/leadler/{id}/donustur")
    @Operation(summary = "Lead'i dönüştür", description = "Lead'i mevcut bir cari hesaba dönüştürür")
    public ResponseEntity<CrmLeadDTO> leadDonustur(@PathVariable Long id, @RequestParam Long cariHesapId) {
        return ResponseEntity.ok(crmGenisletmeService.leadDonustur(id, cariHesapId));
    }

    @DeleteMapping("/leadler/{id}")
    @Operation(summary = "Lead sil", description = "Lead'i siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> leadSil(@PathVariable Long id) {
        crmGenisletmeService.leadSil(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Aktivite ----------------

    @GetMapping("/aktiviteler")
    @Operation(summary = "Aktiviteleri getir", description = "CRM aktivitelerini listeler")
    public ResponseEntity<Page<CrmAktiviteDTO>> aktiviteler(HttpServletRequest request,
                                                            @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(crmGenisletmeService.aktiviteleriGetir(sirketId, pageable));
    }

    @GetMapping("/aktiviteler/cari/{cariHesapId}")
    @Operation(summary = "Cari aktiviteleri", description = "Bir cari hesabın aktivitelerini listeler")
    public ResponseEntity<List<CrmAktiviteDTO>> cariAktiviteleri(@PathVariable Long cariHesapId,
                                                                 HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(crmGenisletmeService.cariAktiviteleri(sirketId, cariHesapId));
    }

    @PostMapping("/aktiviteler")
    @Operation(summary = "Aktivite oluştur", description = "Yeni CRM aktivitesi oluşturur")
    public ResponseEntity<CrmAktiviteDTO> aktiviteOlustur(@Valid @RequestBody CrmAktiviteDTO dto,
                                                          HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(crmGenisletmeService.aktiviteOlustur(dto, sirketId));
    }

    @PutMapping("/aktiviteler/{id}/tamamla")
    @Operation(summary = "Aktiviteyi tamamla", description = "Aktiviteyi tamamlandı/yarım olarak işaretler")
    public ResponseEntity<CrmAktiviteDTO> aktiviteTamamla(@PathVariable Long id,
                                                          @RequestParam(defaultValue = "true") boolean tamamlandi) {
        return ResponseEntity.ok(crmGenisletmeService.aktiviteTamamla(id, tamamlandi));
    }

    @DeleteMapping("/aktiviteler/{id}")
    @Operation(summary = "Aktivite sil", description = "Aktiviteyi siler")
    public ResponseEntity<Void> aktiviteSil(@PathVariable Long id) {
        crmGenisletmeService.aktiviteSil(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Kampanya ----------------

    @GetMapping("/kampanyalar")
    @Operation(summary = "Kampanyaları getir", description = "Pazarlama/satış kampanyalarını listeler")
    public ResponseEntity<Page<CrmKampanyaDTO>> kampanyalar(HttpServletRequest request,
                                                            @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(crmGenisletmeService.kampanyalariGetir(sirketId, pageable));
    }

    @PostMapping("/kampanyalar")
    @Operation(summary = "Kampanya oluştur", description = "Yeni kampanya oluşturur")
    public ResponseEntity<CrmKampanyaDTO> kampanyaOlustur(@Valid @RequestBody CrmKampanyaDTO dto,
                                                          HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(crmGenisletmeService.kampanyaOlustur(dto, sirketId));
    }

    @PutMapping("/kampanyalar/{id}")
    @Operation(summary = "Kampanya güncelle", description = "Kampanya bilgilerini günceller")
    public ResponseEntity<CrmKampanyaDTO> kampanyaGuncelle(@PathVariable Long id,
                                                           @Valid @RequestBody CrmKampanyaDTO dto) {
        return ResponseEntity.ok(crmGenisletmeService.kampanyaGuncelle(id, dto));
    }

    @DeleteMapping("/kampanyalar/{id}")
    @Operation(summary = "Kampanya sil", description = "Kampanyayı siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> kampanyaSil(@PathVariable Long id) {
        crmGenisletmeService.kampanyaSil(id);
        return ResponseEntity.noContent().build();
    }
}
