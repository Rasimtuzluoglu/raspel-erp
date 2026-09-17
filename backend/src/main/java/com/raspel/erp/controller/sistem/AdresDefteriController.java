package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.AdresDefteriDTO;
import com.raspel.erp.service.sistem.AdresDefteriService;
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

@Tag(name = "Adres Defteri", description = "Hizmet kişileri (elektrikçi, tesisatçı vb.) rehber API")
@RestController
@RequestMapping("/api/adres-defteri")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class AdresDefteriController {

    private final AdresDefteriService adresDefteriService;

    @GetMapping
    @Operation(summary = "Adres defteri kayıtlarını getir",
            description = "Arama (ad/telefon/e-posta/adres/etiket), tür ve etiket filtresiyle sayfalı listeler")
    public ResponseEntity<Page<AdresDefteriDTO>> tumu(
            HttpServletRequest request,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "tur", required = false) String tur,
            @RequestParam(value = "etiket", required = false) String etiket,
            @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        if ((q != null && !q.isBlank()) || (tur != null && !tur.isBlank()) || (etiket != null && !etiket.isBlank())) {
            return ResponseEntity.ok(adresDefteriService.filtrele(sirketId, q, tur, etiket, pageable));
        }
        return ResponseEntity.ok(adresDefteriService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/turler")
    @Operation(summary = "Kullanılan meslek/tür listesi", description = "Filtre için mevcut türlerin listesi")
    public ResponseEntity<List<String>> turler(HttpServletRequest request) {
        return ResponseEntity.ok(adresDefteriService.turListesi((Long) request.getAttribute("sirketId")));
    }

    @GetMapping("/etiketler")
    @Operation(summary = "Kullanılan etiket listesi", description = "Filtre için mevcut etiketlerin listesi")
    public ResponseEntity<List<String>> etiketler(HttpServletRequest request) {
        return ResponseEntity.ok(adresDefteriService.etiketListesi((Long) request.getAttribute("sirketId")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre kayıt getir")
    public ResponseEntity<AdresDefteriDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(adresDefteriService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni kayıt oluştur")
    public ResponseEntity<AdresDefteriDTO> olustur(@RequestBody @jakarta.validation.Valid AdresDefteriDTO dto,
                                                   HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(adresDefteriService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Kaydı güncelle")
    public ResponseEntity<AdresDefteriDTO> guncelle(@PathVariable Long id,
                                                    @RequestBody @jakarta.validation.Valid AdresDefteriDTO dto) {
        return ResponseEntity.ok(adresDefteriService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kaydı sil (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        adresDefteriService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
