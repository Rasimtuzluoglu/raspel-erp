package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.OnayBekleyenDTO;
import com.raspel.erp.dto.sistem.OnayKararDTO;
import com.raspel.erp.dto.sistem.OnayKararRequest;
import com.raspel.erp.dto.sistem.OnayOzetDTO;
import com.raspel.erp.dto.sistem.OnayTopluRequest;
import com.raspel.erp.dto.sistem.OnayTopluSonucDTO;
import com.raspel.erp.service.sistem.OnayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Birleşik onay API'si.
 *
 * <p>Onaylayan kimliği istemciden alınmaz; JWT/istek bağlamından
 * ({@code kullaniciId}, {@code displayName}) okunur. Tüm uçlar ADMIN ve
 * MUHASEBE rolleriyle sınırlıdır.
 */
@Tag(name = "Onaylar", description = "Birleşik onay merkezi: özet, bekleyenler, karar, toplu karar, geçmiş")
@RestController
@RequestMapping("/api/onaylar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
public class OnayController {

    /** Tek istekte dönebilecek en fazla kayıt. */
    private static final int MAKS_SAYFA_BOYUTU = 200;

    private final OnayService onayService;

    @GetMapping("/ozet")
    @Operation(summary = "Bekleyen onay özeti", description = "Tür bazında bekleyen onay sayılarını ve toplamı döner")
    public ResponseEntity<OnayOzetDTO> ozet(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(onayService.ozet(sirketId));
    }

    @GetMapping("/bekleyen")
    @Operation(summary = "Bekleyen onaylar",
            description = "Tür (İzin/Masraf/Satınalma/Sipariş), arama ve sayfalama ile bekleyen onayları listeler")
    public ResponseEntity<Page<OnayBekleyenDTO>> bekleyen(
            HttpServletRequest request,
            @RequestParam(required = false) String tur,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(onayService.bekleyen(sirketId, tur, q, sayfaOlustur(page, size)));
    }

    @PostMapping("/karar")
    @Operation(summary = "Tek kayıt onay/red", description = "Seçili kayıt için onay veya red kararı verir; RED'de gerekçe zorunludur")
    public ResponseEntity<OnayKararDTO> karar(HttpServletRequest request,
                                             @Valid @RequestBody OnayKararRequest body) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(onayService.karar(sirketId, kullaniciId, kullaniciAdi(request), body));
    }

    @PostMapping("/toplu")
    @Operation(summary = "Toplu onay/red", description = "Aynı türde birden çok kaydı karara bağlar; kayıt bazında kısmi sonuç döner")
    public ResponseEntity<OnayTopluSonucDTO> toplu(HttpServletRequest request,
                                                   @Valid @RequestBody OnayTopluRequest body) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(onayService.toplu(sirketId, kullaniciId, kullaniciAdi(request), body));
    }

    @GetMapping("/gecmis")
    @Operation(summary = "Karar geçmişi",
            description = "Tür, karar, arama ve tarih aralığı filtreleriyle onay/red karar geçmişini listeler")
    public ResponseEntity<Page<OnayKararDTO>> gecmis(
            HttpServletRequest request,
            @RequestParam(required = false) String tur,
            @RequestParam(required = false) String karar,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(onayService.gecmis(sirketId, tur, karar, q, baslangic, bitis,
                sayfaOlustur(page, size)));
    }

    private Pageable sayfaOlustur(int page, int size) {
        int sayfa = Math.max(page, 0);
        int boyut = size > 0 ? Math.min(size, MAKS_SAYFA_BOYUTU) : 20;
        return PageRequest.of(sayfa, boyut);
    }

    /** Onaylayan adı: görünen ad varsa o, yoksa kullanıcı adı. */
    private String kullaniciAdi(HttpServletRequest request) {
        Object ad = request.getAttribute("displayName");
        if (ad == null || ad.toString().isBlank()) {
            ad = request.getAttribute("username");
        }
        return ad != null ? ad.toString() : null;
    }
}
