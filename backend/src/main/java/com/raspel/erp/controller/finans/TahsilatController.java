package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.TahsilatDTO;
import com.raspel.erp.service.finans.TahsilatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Tag(name = "Tahsilat", description = "Tahsilat ve alacak yönetimi API")
@RestController
@RequestMapping("/api/tahsilat")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class TahsilatController {

    private final TahsilatService tahsilatService;

    @GetMapping
    @Operation(summary = "Tahsilat özeti", description = "Ödenmemiş alacakların cari bazlı yaşlandırma özetini getirir")
    public ResponseEntity<TahsilatDTO> ozet(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(tahsilatService.ozetGetir(sirketId));
    }

    @GetMapping("/gecmis")
    @Operation(summary = "Tahsilat geçmişi", description = "Yapılan tüm tahsilat hareketlerini (ödeme yöntemi ve taksit bilgisiyle) sayfalı getirir")
    public ResponseEntity<org.springframework.data.domain.Page<com.raspel.erp.dto.finans.HareketDTO>> gecmis(
            HttpServletRequest request,
            @PageableDefault(size = 25) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(tahsilatService.gecmis(sirketId, pageable));
    }

    @PostMapping("/{cariId}/hatirlat")
    @Operation(summary = "Hatırlatma gönder", description = "Cariye ait ödenmemiş faturalar için e-posta hatırlatması gönderir")
    public ResponseEntity<Map<String, Object>> hatirlat(HttpServletRequest request, @PathVariable Long cariId) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        int gonderilen = tahsilatService.hatirlat(cariId, sirketId);
        return ResponseEntity.ok(Map.of("gonderilen", gonderilen));
    }

    @PostMapping
    @Operation(summary = "Tahsilat gir", description = "Cariye ait açık faturalara ödeme tahsis eder; ödeme yöntemi (NAKIT/KART/TAKSIT/HAVALE) ve taksit bilgisi kaydedilir")
    public ResponseEntity<Map<String, Object>> tahsilatGir(
            @RequestBody @jakarta.validation.Valid TahsilatGirisDTO dto,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Map<String, Object> sonuc = tahsilatService.tahsilatGir(
                dto.getCariId(), dto.getTutar(), dto.getOdemeYontemi(),
                dto.getTaksitKurum(), dto.getTaksitTutar(), dto.getAciklama(),
                dto.getHareketTarihi(), sirketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(sonuc);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TahsilatGirisDTO {
        @NotNull(message = "Cari hesap seçilmelidir")
        private Long cariId;
        @NotNull(message = "Tutar girilmelidir")
        @DecimalMin(value = "0.01", message = "Tutar 0'dan büyük olmalıdır")
        private java.math.BigDecimal tutar;
        /** Ödeme yöntemi: NAKIT, KART, TAKSIT, HAVALE */
        private String odemeYontemi;
        /** Taksit çekilen banka / finans kurumu adı */
        private String taksitKurum;
        /** Taksit olarak çekilen tutar */
        private java.math.BigDecimal taksitTutar;
        /** Hareket açıklaması */
        @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
        private String aciklama;
        /** Tahsilat tarihi (opsiyonel, boşsa bugün) */
        private java.time.LocalDate hareketTarihi;
    }
}
