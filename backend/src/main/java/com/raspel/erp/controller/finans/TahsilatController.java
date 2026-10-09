package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.TahsilatDTO;
import com.raspel.erp.service.finans.TahsilatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
public class TahsilatController {

    private final TahsilatService tahsilatService;

    // REDTEAM C10: Tahsilat yazan uçlar yetki kodu ile de korunuyor; DRIVER'a
    // FINANS_WRITE verilmez (YetkiService seed'i).
    private final com.raspel.erp.config.security.YetkiKontrol yetkiKontrol;

    private final com.raspel.erp.service.sistem.IdempotencyService idempotencyService;

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
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<Map<String, Object>> hatirlat(HttpServletRequest request, @PathVariable Long cariId) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        int gonderilen = tahsilatService.hatirlat(cariId, sirketId);
        return ResponseEntity.ok(Map.of("gonderilen", gonderilen));
    }

    @PostMapping
    @Operation(summary = "Tahsilat gir", description = "Cariye ait açık faturalara ödeme tahsis eder. Saha personeli (USER) da tahsilat kaydedebilir.")
    // REDTEAM C10: DRIVER rolü burada YANLIŞLIKLA bulunuyordu. DRIVER
    // ("şoför") yalnızca SIPARIS_READ / CARI_READ / STOK_READ yetkilerine sahip
    // (YetkiService.seedKontrolu). Bu uç kasa/banka bakiyesini, cari bakiyeyi ve
    // fatura odenenTutar/kalanTutar alanlarını kalıcı olarak değiştiriyor.
    // Testte kanıtlandı: DRIVER ile 50.000 TL tahsilat girildi, kasa
    // 100.000 -> 150.000, cari 5.000 -> 55.000. Şoförün finansal yazma yetkisi
    // OLMAMALI. Tahsilat yetkisi FINANS_WRITE ile korunur.
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE') or @yetkiKontrol.kontrol(authentication, 'FINANS_WRITE')")
    public ResponseEntity<Map<String, Object>> tahsilatGir(
            Authentication authentication,
            @RequestBody @Valid TahsilatGirisDTO dto,
            HttpServletRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        Long sirketId = (Long) request.getAttribute("sirketId");

        // Çift kayıt koruması. Tahsilat cari bakiyeyi, kasa/banka bakiyesini ve
        // fatura odenen/kalan tutarlarını kalıcı olarak değiştirir; aynı isteğin
        // iki kez işlenmesi çift tahsilat demektir. Anahtarı kaydetmeden ÖNCE
        // rezerve et (FaturaController ile aynı desen).
        String anahtar = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            anahtar = "idem:tahsilat:" + (sirketId != null ? sirketId : 0L) + ":" + idempotencyKey.trim();
            if (!idempotencyService.deneKilit(anahtar)) {
                // Aynı anahtarla ikinci istek: ya ilk istek hâlâ sürüyor ya da
                // bu TTL içinde tamamlanmış. İkisinde de tekrar tahsilat YAPILMAZ.
                throw new com.raspel.erp.exception.BusinessException(
                        "Bu tahsilat kaydı zaten işleniyor veya kaydedilmiş. Mükerrer kayıt oluşmadı; "
                        + "tahsilat geçmişinden kontrol edebilirsiniz.");
            }
        }

        try {
            Map<String, Object> sonuc = tahsilatService.tahsilatGir(
                    dto.getCariId(), dto.getTutar(), dto.getOdemeYontemi(),
                    dto.getTaksitKurum(), dto.getTaksitTutar(), dto.getAciklama(),
                    dto.getHareketTarihi(), sirketId,
                    dto.getPosTerminaliId(), dto.getKomisyonTutar(), dto.getValorTarihi(),
                    dto.getTaksitId(), dto.getKasaId(), dto.getBankaId());
            if (anahtar != null) {
                Object hareketId = sonuc.get("hareketId");
                if (hareketId instanceof Number n) {
                    idempotencyService.tamamla(anahtar, n.longValue());
                } else {
                    // Sonuç okunabilir değilse kilidi hemen bırak: yeniden denemede
                    // yeni tahsilat yapılabilsin (sunucu tarafında zaten tutar
                    // doğrulaması ve dönem kilidi var).
                    idempotencyService.serbestBirak(anahtar);
                }
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(sonuc);
        } catch (RuntimeException e) {
            if (anahtar != null) idempotencyService.serbestBirak(anahtar);
            throw e;
        }
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
        /** Odemenin atanacagi taksit plan kalemi (opsiyonel) */
        private Long taksitId;
        /** POS terminali ID (kart tek çekim) */
        private Long posTerminaliId;
        /** Kart komisyon tutarı */
        private java.math.BigDecimal komisyonTutar;
        /** Valör (bankaya geçiş) tarihi */
        private java.time.LocalDate valorTarihi;
        /** Hareket açıklaması */
        @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
        private String aciklama;
        /** Tahsilat tarihi (opsiyonel, boşsa bugün) */
        private java.time.LocalDate hareketTarihi;
        /** Tahsilatın giriş yapılacağı kasa (NAKIT için) */
        private Long kasaId;
        /** Tahsilatın giriş yapılacağı banka hesabı (HAVALE/KART için) */
        private Long bankaId;
    }
}
