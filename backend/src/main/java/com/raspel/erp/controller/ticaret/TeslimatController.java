package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.SurucuDTO;
import com.raspel.erp.dto.ticaret.TeslimatDTO;
import com.raspel.erp.dto.ticaret.TeslimatDurumLogDTO;
import com.raspel.erp.service.ticaret.TeslimatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Teslimat", description = "Teslimat takibi API")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'DRIVER')")
public class TeslimatController {

    private final TeslimatService teslimatService;

    @GetMapping("/api/drivers")
    @Operation(summary = "Şoförleri listele", description = "DRIVER rolündeki kullanıcıları listeler")
    public ResponseEntity<List<SurucuDTO>> suruculer(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(teslimatService.suruculer(sirketId, kullaniciId));
    }

    @PostMapping("/api/deliveries")
    @Operation(summary = "Teslimat oluştur", description = "Fatura oluşturulurken teslimat kaydı açar (saleId, driverId, adres)")
    public ResponseEntity<TeslimatDTO> olustur(@Valid @RequestBody TeslimatDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(teslimatService.olustur(dto, sirketId));
    }

    @GetMapping("/api/deliveries/by-driver")
    @Operation(summary = "Şoför bazlı özet", description = "Tüm şoförleri bekleyen teslimat sayılarıyla getirir")
    public ResponseEntity<List<SurucuDTO>> byDriver(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(teslimatService.suruculer(sirketId, kullaniciId));
    }

    @GetMapping("/api/deliveries")
    @Operation(summary = "Şoförün teslimatları", description = "Bir şoförün tüm teslimat noktalarını getirir")
    public ResponseEntity<List<TeslimatDTO>> teslimatlar(
            @RequestParam Long driverId,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(teslimatService.teslimatlar(sirketId, driverId, kullaniciId));
    }

    @PatchMapping("/api/deliveries/{id}/status")
    @Operation(summary = "Teslimat durumunu güncelle", description = "Teslimat durumunu günceller (BEKLEMEDE, YOLDA, TESLIM_EDILDI, IPTAL)")
    public ResponseEntity<TeslimatDTO> durumGuncelle(
            @PathVariable Long id,
            @RequestBody DurumRequest body,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(teslimatService.durumGuncelle(id, body != null ? body.durum() : null, sirketId, kullaniciId));
    }

    @PostMapping("/api/deliveries/{id}/foto")
    @Operation(summary = "Teslimat fotoğrafı yükle", description = "Teslimata fotoğraf iliştirir (ispat amaçlı)")
    public ResponseEntity<TeslimatDTO> fotoYukle(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(teslimatService.fotoYukle(id, file, sirketId, kullaniciId));
    }

    @GetMapping("/api/deliveries/{id}/gecmis")
    @Operation(summary = "Teslimat durum geçmişi", description = "Teslimatın durum değişim geçmişini getirir")
    public ResponseEntity<List<TeslimatDurumLogDTO>> gecmis(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(teslimatService.gecmis(id, sirketId, kullaniciId));
    }

    @GetMapping("/api/deliveries/{id}")
    @Operation(summary = "Teslimat detayı", description = "Tek bir teslimatın detayını getirir")
    public ResponseEntity<TeslimatDTO> teslimatDetay(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(teslimatService.getir(id, sirketId, kullaniciId));
    }

    @PostMapping("/api/deliveries/{id}/teslim")
    @Operation(summary = "Dijital teslimat (imzalı)", description = "Teslim alan adı ve dijital imza ile teslimatı tamamlar")
    public ResponseEntity<TeslimatDTO> teslimEt(
            @PathVariable Long id,
            @RequestParam(value = "teslimAlanAd", required = false) String teslimAlanAd,
            @RequestParam(value = "teslimNotu", required = false) String teslimNotu,
            @RequestParam(value = "teslimKonum", required = false) String teslimKonum,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        var istek = new com.raspel.erp.service.ticaret.TeslimatService.TeslimIstegi(teslimAlanAd, teslimNotu, teslimKonum);
        return ResponseEntity.ok(teslimatService.teslimEt(id, istek, file, sirketId, kullaniciId));
    }

    @PostMapping("/api/deliveries/siparis/{siparisId}/teslim")
    @Operation(summary = "Sipariş dijital teslimat", description = "Saha portalı için sipariş teslimatını imzayla tamamlar")
    public ResponseEntity<TeslimatDTO> teslimEtSiparis(
            @PathVariable Long siparisId,
            @RequestParam(value = "teslimAlanAd", required = false) String teslimAlanAd,
            @RequestParam(value = "teslimNotu", required = false) String teslimNotu,
            @RequestParam(value = "teslimKonum", required = false) String teslimKonum,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        var istek = new com.raspel.erp.service.ticaret.TeslimatService.TeslimIstegi(teslimAlanAd, teslimNotu, teslimKonum);
        return ResponseEntity.ok(teslimatService.teslimEtSiparis(siparisId, istek, file, sirketId, kullaniciId));
    }

    @GetMapping("/api/deliveries/{id}/fis")
    @Operation(summary = "Teslimat fişi PDF", description = "Dijital teslimat fişini PDF olarak üretir")
    public ResponseEntity<byte[]> teslimatFisi(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        byte[] pdf = teslimatService.teslimatFisiPdf(id, sirketId, kullaniciId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"teslimat-fisi-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    record DurumRequest(String durum) {}
}
