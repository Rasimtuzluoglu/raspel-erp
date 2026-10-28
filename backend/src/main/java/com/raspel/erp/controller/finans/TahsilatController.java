package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.TahsilatDTO;
import com.raspel.erp.dto.finans.TahsilatHedefDTO;
import com.raspel.erp.dto.finans.TahsilatIptalIstek;
import com.raspel.erp.dto.finans.TahsilatIslemDTO;
import com.raspel.erp.dto.finans.TahsilatPerformansDTO;
import com.raspel.erp.dto.finans.TopluHatirlatmaIstek;
import com.raspel.erp.dto.finans.TopluHatirlatmaSonuc;
import com.raspel.erp.service.finans.TahsilatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    @Operation(summary = "Tahsilat özeti", description = "Ödenmemiş alacakların cari bazlı yaşlandırma özetini getirir; q/aralik/sadeceGecikmis/minTutar filtreleri yalnız cari listesini daraltır")
    public ResponseEntity<TahsilatDTO> ozet(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String aralik,
            @RequestParam(required = false) Boolean sadeceGecikmis,
            @RequestParam(required = false) BigDecimal minTutar) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(tahsilatService.ozetGetir(sirketId, q, aralik, sadeceGecikmis, minTutar));
    }

    @GetMapping("/gecmis")
    @Operation(summary = "Tahsilat geçmişi", description = "Tahsilatları işlem (batch) bazında, sunucu filtreleri ve sayfalamayla getirir")
    public ResponseEntity<Page<TahsilatIslemDTO>> gecmis(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long cariId,
            @RequestParam(required = false) String odemeYontemi,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) Long kasaId,
            @RequestParam(required = false) Long bankaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @PageableDefault(size = 25, sort = "tarih", direction = Sort.Direction.DESC) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(tahsilatService.gecmisFiltreli(
                sirketId, q, cariId, odemeYontemi, durum, kasaId, bankaId, baslangic, bitis, pageable));
    }

    @PostMapping("/islem/{id}/iptal")
    @Operation(summary = "Tahsilat işlemini iptal et", description = "İşlemdeki tüm hareketleri gerekçeli olarak geri alır (ADMIN/MUHASEBE veya FINANS_WRITE)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE') or @yetkiKontrol.kontrol(authentication, 'FINANS_WRITE')")
    public ResponseEntity<TahsilatIslemDTO> iptal(HttpServletRequest request,
                                                  @PathVariable Long id,
                                                  @RequestBody @Valid TahsilatIptalIstek istek) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        // sirketId servis içinde işlem kaydından doğrulanır (tenant izolasyonu).
        return ResponseEntity.ok(tahsilatService.iptalEt(id, istek.getGerekce(), kullaniciId));
    }

    @PutMapping("/hedef")
    @Operation(summary = "Aylık tahsilat hedefi kaydet", description = "Aynı yıl/ay için upsert eder (ADMIN/MUHASEBE)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<TahsilatHedefDTO> hedef(HttpServletRequest request,
                                                  @RequestBody @Valid TahsilatHedefDTO dto) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(tahsilatService.hedefKaydet(
                sirketId, dto.getYil(), dto.getAy(), dto.getTutar(), kullaniciId));
    }

    @GetMapping("/performans")
    @Operation(summary = "Tahsilat hedef performansı", description = "Seçili ayın hedefi, iptal hariç tahsilat toplamı, gerçekleşme yüzdesi ve 6 aylık trend")
    public ResponseEntity<TahsilatPerformansDTO> performans(
            HttpServletRequest request,
            @RequestParam(required = false) Integer yil,
            @RequestParam(required = false) Integer ay) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(tahsilatService.performans(sirketId, yil, ay));
    }

    @PostMapping("/toplu-hatirlat")
    @Operation(summary = "Toplu e-posta hatırlat", description = "Seçili carilere ödeme hatırlatma e-postası gönderir; e-postasız cariler atlanır")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<TopluHatirlatmaSonuc> topluHatirlat(
            HttpServletRequest request,
            @RequestBody @Valid TopluHatirlatmaIstek istek) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(tahsilatService.topluHatirlat(sirketId, istek.getCariIds()));
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
                    dto.getTaksitId(), dto.getKasaId(), dto.getBankaId(),
                    (Long) request.getAttribute("kullaniciId"));
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
