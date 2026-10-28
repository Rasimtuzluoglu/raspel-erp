package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.DashboardAkisDTO;
import com.raspel.erp.dto.sistem.DashboardDTO;
import com.raspel.erp.dto.sistem.DashboardGorevDTO;
import com.raspel.erp.dto.sistem.DashboardGrafikDTO;
import com.raspel.erp.dto.sistem.DashboardIcgoruDTO;
import com.raspel.erp.dto.sistem.DashboardOzetDTO;
import com.raspel.erp.dto.sistem.DashboardTercihDTO;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.service.sistem.DashboardService;
import com.raspel.erp.service.sistem.DashboardTercihService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dashboard Controller
 * Dashboard'un ihtiyaç duyduğu veriler için API endpoint'lerini sağlar.
 *
 * <p>Legacy {@code GET /api/dashboard} sözleşmesi korunur; Anasayfa revizyonu
 * veriyi parçalı yükler ({@code /ozet}, {@code /gorevler}, {@code /grafikler},
 * {@code /akis}, {@code /icgoruler}).
 */
@Tag(name = "Dashboard", description = "Dashboard verileri API")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardTercihService dashboardTercihService;

    // ------------------------------------------------------------------
    // Legacy uç — payload DEĞİŞMEZ
    // ------------------------------------------------------------------

    @GetMapping
    @Operation(summary = "Dashboard verilerini getir", description = "Dashboard için özet istatistikleri ve grafik verilerini döndürür")
    public ResponseEntity<DashboardDTO> dashboardVerileriGetir(HttpServletRequest request) {
        log.info("GET /api/dashboard - Dashboard verileri getiriliyor");
        Long sirketId = (Long) request.getAttribute("sirketId");
        DashboardDTO dashboardDTO = dashboardService.dashboardVerileriGetir(sirketId);
        return ResponseEntity.ok(dashboardDTO);
    }

    // ------------------------------------------------------------------
    // Parçalı uçlar
    // ------------------------------------------------------------------

    @GetMapping("/ozet")
    @Operation(summary = "Anasayfa hızlı özeti", description = "Hero, bugün şeridi, KPI'lar, stok ve görev sayaçlarını döndürür")
    public ResponseEntity<DashboardOzetDTO> ozetGetir(HttpServletRequest request) {
        Long sirketId = sirketId(request);
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(dashboardService.ozetGetir(
                sirketId, kullaniciId, kullaniciAdi(request), aktifRol()));
    }

    @GetMapping("/gorevler")
    @Operation(summary = "Görev merkezi", description = "Rol filtreli, öncelik sıralı görev kartlarını döndürür")
    public ResponseEntity<List<DashboardGorevDTO>> gorevlerGetir(HttpServletRequest request) {
        return ResponseEntity.ok(dashboardService.gorevlerGetir(sirketId(request), aktifRol()));
    }

    @GetMapping("/grafikler")
    @Operation(summary = "Anasayfa grafik verileri", description = "Gelir/gider, nakit akışı, ürün/kategori, yaşlandırma ve hedef verilerini döndürür")
    public ResponseEntity<DashboardGrafikDTO> grafiklerGetir(HttpServletRequest request) {
        return ResponseEntity.ok(dashboardService.grafiklerGetir(sirketId(request)));
    }

    @GetMapping("/akis")
    @Operation(summary = "Canlı işlem akışı", description = "Onay, tahsilat, ödeme, fatura, sipariş ve stok olaylarını birleşik zaman çizelgesinde döndürür")
    public ResponseEntity<List<DashboardAkisDTO>> akisGetir(
            HttpServletRequest request,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(dashboardService.akisGetir(sirketId(request), limit));
    }

    @GetMapping("/icgoruler")
    @Operation(summary = "Kural tabanlı içgörüler", description = "Ciro hedefi, vadesi geçen fatura, kritik stok ve likidite trendi kurallarını döndürür")
    public ResponseEntity<List<DashboardIcgoruDTO>> icgorulerGetir(HttpServletRequest request) {
        return ResponseEntity.ok(dashboardService.icgorulerGetir(sirketId(request)));
    }

    // ------------------------------------------------------------------
    // Kullanıcı düzeni (cache'lenmez)
    // ------------------------------------------------------------------

    @GetMapping("/tercihler")
    @Operation(summary = "Anasayfa düzenini getir", description = "Kullanıcının kayıtlı düzenini döndürür; kayıt yoksa duzen=null")
    public ResponseEntity<DashboardTercihDTO> tercihGetir(HttpServletRequest request) {
        return ResponseEntity.ok(dashboardTercihService.tercihGetir(kullaniciId(request), sirketId(request)));
    }

    @PutMapping("/tercihler")
    @Operation(summary = "Anasayfa düzenini kaydet", description = "Kullanıcının düzenini upsert eder (JSON object, en fazla 20 KB)")
    public ResponseEntity<DashboardTercihDTO> tercihKaydet(HttpServletRequest request,
                                                           @RequestBody DashboardTercihDTO dto) {
        return ResponseEntity.ok(dashboardTercihService.tercihKaydet(kullaniciId(request), sirketId(request), dto));
    }

    // ------------------------------------------------------------------
    // İstek bağlamı yardımcıları
    // ------------------------------------------------------------------

    /** Tenant zorunludur; eksikse veri sızdırmak yerine hata döner. */
    private Long sirketId(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        if (sirketId == null) {
            throw new BusinessException("Şirket bilgisi bulunamadı");
        }
        return sirketId;
    }

    private Long kullaniciId(HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        if (kullaniciId == null) {
            throw new BusinessException("Kullanıcı bilgisi bulunamadı");
        }
        return kullaniciId;
    }

    /** Görünen ad: JWT/API token filtresinin yazdığı displayName → username. */
    private String kullaniciAdi(HttpServletRequest request) {
        Object displayName = request.getAttribute("displayName");
        if (displayName instanceof String ad && !ad.isBlank()) {
            return ad;
        }
        Object username = request.getAttribute("username");
        if (username instanceof String ad && !ad.isBlank()) {
            return ad;
        }
        return "Kullanıcı";
    }

    /** SecurityContext authority'lerinden rol (ADMIN > MUHASEBE > USER). */
    private String aktifRol() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "USER";
        }
        var roller = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        if (roller.contains("ROLE_ADMIN")) {
            return "ADMIN";
        }
        if (roller.contains("ROLE_MUHASEBE")) {
            return "MUHASEBE";
        }
        return "USER";
    }
}
