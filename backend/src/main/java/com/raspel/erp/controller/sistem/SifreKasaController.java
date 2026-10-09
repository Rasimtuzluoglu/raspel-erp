package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.SifreKasaDTO;
import com.raspel.erp.dto.sistem.SifreKasaOzetDTO;
import com.raspel.erp.service.sistem.SifreKasaService;
import com.raspel.erp.util.IstekYardimci;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Sifre kasasi API.
 *
 * <p>GUVENLIK:
 * <ul>
 *   <li>Liste/ozet uclari sifre metnini DONMEZ; yalniz metadana erisir.</li>
 *   <li>Sifre yalniz {@code GET /{id}/sifre} ile acilir; her cagri
 *       denetim izine "SIFRE_GORUNTULENME" olarak yazilir.</li>
 *   <li>Acma yanitina {@code Cache-Control: no-store} eklenir; sifre
 *       tarayici/proxy onbellegine dusmez.</li>
 *   <li>Global kasa yalnizca ADMIN tarafindan yazilir; bu kisit serviste
 *       uygulanir, controller yalnizca rol kapisini tutar.</li>
 * </ul>
 *
 * <p>SAHA/DRIVER rolleri router/sidebar guard'lari tarafindan zaten
 * disarida tutulur; sinif kurali onlari da kapsam disi birakir.
 */
@Tag(name = "Sifre Kasasi", description = "Kullanici ve sirket sifre kasasi API")
@RestController
@RequestMapping("/api/sifre-kasa")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
public class SifreKasaController {

    private final SifreKasaService sifreKasaService;

    @GetMapping
    @Operation(summary = "Kasa listesi", description = "Kisisel + sirket sifre kayitlarinin metadatasini doner (sifre metni yok)")
    public ResponseEntity<List<SifreKasaDTO>> listele(
            @RequestParam(required = false) String kapsam,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) String kategori,
            @RequestParam(required = false, name = "q") String arama,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        Boolean aktif = durumToAktif(durum);
        return ResponseEntity.ok(sifreKasaService.listele(
                sirketId, kullaniciId, kapsam, aktif, kategori, arama, request.isUserInRole("ADMIN")));
    }

    @GetMapping("/ozet")
    @Operation(summary = "Kasa ozeti", description = "Sidebar rozeti icin uyari/suresi bitmis/sirket geneli/arsiv sayilari")
    public ResponseEntity<SifreKasaOzetDTO> ozet(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(sifreKasaService.ozet(sirketId, kullaniciId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Kayit detayi", description = "Metadatalari doner; sifre metni yok")
    public ResponseEntity<SifreKasaDTO> idyeGore(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(sifreKasaService.idyeGoreGetir(id, sirketId, kullaniciId));
    }

    @GetMapping("/{id}/sifre")
    @Operation(summary = "Sifreyi ac", description = "Sifre metnini duz olarak doner. Her cagri denetim izine yazilir.")
    public ResponseEntity<Map<String, String>> sifreyiAc(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String ip = IstekYardimci.istemciIp(request);
        String sifre = sifreKasaService.sifreyiGoster(
                id, sirketId, kullaniciId, request.isUserInRole("ADMIN"), ip);
        return ResponseEntity.ok()
                // Sifre asla onbellege alinmamali.
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header("Pragma", "no-cache")
                .body(Map.of("sifre", sifre));
    }

    @PostMapping
    @Operation(summary = "Yeni kayit", description = "Kisisel kayit: herkes. Global kayit: yalnizca yonetici.")
    public ResponseEntity<SifreKasaDTO> olustur(@Valid @RequestBody SifreKasaDTO dto,
                                                 HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sifreKasaService.olustur(dto, sirketId, kullaniciId, request.isUserInRole("ADMIN")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Kayit guncelle", description = "Sifre bos birakilirsa mevcut sifre korunur")
    public ResponseEntity<SifreKasaDTO> guncelle(@PathVariable Long id,
                                                  @Valid @RequestBody SifreKasaDTO dto,
                                                  HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(sifreKasaService.guncelle(
                id, dto, sirketId, kullaniciId, request.isUserInRole("ADMIN")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kaydi arsivle", description = "Fiziksel silme yok; kayit arsive alinir ve geri alinabilir")
    public ResponseEntity<Void> arsivle(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        sifreKasaService.arsivle(id, sirketId, kullaniciId, request.isUserInRole("ADMIN"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/geri-al")
    @Operation(summary = "Arsivden geri al", description = "Arsivlenen kaydi yeniden aktiflestirir")
    public ResponseEntity<Void> arsivleGeriAl(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        sifreKasaService.arsivleGeriAl(id, sirketId, kullaniciId, request.isUserInRole("ADMIN"));
        return ResponseEntity.noContent().build();
    }

    /** durum: AKTIF (varsayilan) | ARSIV | TUMU -> repository aktif filtresi. */
    private Boolean durumToAktif(String durum) {
        if (durum == null || durum.isBlank() || "AKTIF".equalsIgnoreCase(durum)) return true;
        if ("ARSIV".equalsIgnoreCase(durum)) return false;
        if ("TUMU".equalsIgnoreCase(durum)) return null;
        return true;
    }
}