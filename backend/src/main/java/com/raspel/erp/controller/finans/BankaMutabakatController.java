package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.BankaHareketiDTO;
import com.raspel.erp.service.finans.BankaMutabakatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Banka Mutabakatı", description = "Banka hesap özeti yükleme ve fatura eşleştirme API")
@RestController
@RequestMapping("/api/bankalar/{bankaId}/mutabakat")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class BankaMutabakatController {

    private final BankaMutabakatService bankaMutabakatService;

    @GetMapping
    @Operation(summary = "Banka hareketlerini listele", description = "Bankaya yüklenen hareketleri ve eşleşme durumunu listeler")
    public ResponseEntity<List<BankaHareketiDTO>> listele(@PathVariable Long bankaId) {
        return ResponseEntity.ok(bankaMutabakatService.listele(bankaId));
    }

    @PostMapping("/yukle")
    @Operation(summary = "Hesap özeti yükle", description = "CSV/Excel hesap özetini yükler ve faturalarla otomatik eşleştirir (tarih;aciklama;borc;alacak;bakiye)")
    public ResponseEntity<List<BankaHareketiDTO>> yukle(@PathVariable Long bankaId,
                                                        @RequestParam("dosya") MultipartFile dosya,
                                                        HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        bankaMutabakatService.yukle(bankaId, dosya, sirketId);
        return ResponseEntity.ok(bankaMutabakatService.listele(bankaId));
    }

    @PostMapping("/otomatik-eslestir")
    @Operation(summary = "Otomatik eşleştir", description = "Eşleşmemiş hareketleri tutar ve tarih benzerliğine göre faturalarla eşleştirir")
    public ResponseEntity<List<BankaHareketiDTO>> otomatikEslestir(@PathVariable Long bankaId, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(bankaMutabakatService.otomatikEslestir(bankaId, sirketId));
    }

    @PostMapping("/{hareketId}/eslestir/{faturaId}")
    @Operation(summary = "Manuel eşleştir", description = "Bir banka hareketini elle bir fatura ile eşleştirir")
    public ResponseEntity<BankaHareketiDTO> eslestir(@PathVariable Long hareketId, @PathVariable Long faturaId) {
        return ResponseEntity.ok(bankaMutabakatService.eslestir(hareketId, faturaId));
    }

    @PostMapping("/{hareketId}/eslestirmeyi-kaldir")
    @Operation(summary = "Eşleştirmeyi kaldır", description = "Banka hareketinin fatura eşleşmesini kaldırır")
    public ResponseEntity<BankaHareketiDTO> eslestirmeyiKaldir(@PathVariable Long hareketId) {
        return ResponseEntity.ok(bankaMutabakatService.eslestirmeyiKaldir(hareketId));
    }

    @DeleteMapping
    @Operation(summary = "Hareketleri temizle", description = "Bankaya yüklenen tüm mutabakat hareketlerini siler")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long bankaId) {
        bankaMutabakatService.sil(bankaId);
        return ResponseEntity.noContent().build();
    }
}
