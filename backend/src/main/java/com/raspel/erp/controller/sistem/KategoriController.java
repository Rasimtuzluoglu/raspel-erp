package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.*;
import com.raspel.erp.entity.sistem.KategoriKapsam;
import com.raspel.erp.service.sistem.KategoriService;
import jakarta.servlet.http.HttpServletRequest;
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

@Tag(name = "Kategoriler", description = "Merkezi kategori registry API")
@RestController
@RequestMapping("/api/kategoriler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class KategoriController {

    private final KategoriService kategoriService;

    /**
     * Sayfalı düz liste. Geriye uyumluluk için parametresiz çağrı tüm
     * kapsamları listeler; yeni parametreler opsiyoneldir.
     */
    @GetMapping
    @Operation(summary = "Kategorileri listele", description = "Kapsam/arama/aktiflik filtreleriyle sayfalı liste")
    public ResponseEntity<Page<KategoriDTO>> tumu(
            HttpServletRequest request,
            @RequestParam(required = false) String kapsam,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean aktif,
            @RequestParam(required = false) Long ustKategoriId,
            @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(kategoriService.listele(
                sirketId, kapsamCoz(kapsam), q, aktif, ustKategoriId, pageable));
    }

    @GetMapping("/agac")
    @Operation(summary = "Kategori ağacı", description = "2 seviyeli ağaç ve kullanım sayıları")
    public ResponseEntity<List<KategoriAgacDTO>> agac(
            HttpServletRequest request,
            @RequestParam(required = false) String kapsam,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean aktif) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(kategoriService.agac(sirketId, kapsamCoz(kapsam), q, aktif));
    }

    @GetMapping("/ozet")
    @Operation(summary = "Kategori özeti", description = "Toplam / aktif / pasif / alt kategori sayıları")
    public ResponseEntity<KategoriOzetDTO> ozet(
            HttpServletRequest request,
            @RequestParam(required = false) String kapsam) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(kategoriService.ozet(sirketId, kapsamCoz(kapsam)));
    }

    @GetMapping("/secenekler")
    @Operation(summary = "Kategori seçenekleri", description = "Aktif kategoriler, üst kategoriye göre gruplu")
    public ResponseEntity<List<KategoriAgacDTO>> secenekler(
            HttpServletRequest request,
            @RequestParam(required = false) String kapsam) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(kategoriService.secenekler(sirketId, kapsamCoz(kapsam)));
    }

    @GetMapping("/sistem-kategorileri")
    @Operation(summary = "Sistem kategorileri", description = "Personel masraf kodları (salt-okunur)")
    public ResponseEntity<List<SistemKategoriDTO>> sistemKategorileri() {
        return ResponseEntity.ok(kategoriService.sistemKategorileri());
    }

    /**
     * Geriye uyumluluk: GELIR/GIDER (veya KASA_GELIR/KASA_GIDER) türüne göre
     * kasa kategorileri.
     */
    @GetMapping("/tur/{tur}")
    @Operation(summary = "Türe göre kategoriler", description = "Kasa gelir/gider kategorileri")
    public ResponseEntity<List<KategoriDTO>> turuGetir(@PathVariable String tur, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(kategoriService.turuGetir(tur, sirketId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Kategori detayı")
    public ResponseEntity<KategoriDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(kategoriService.getir(id));
    }

    @GetMapping("/{id}/kullanim")
    @Operation(summary = "Kategori kullanımı", description = "Modül bazlı kullanım sayıları")
    public ResponseEntity<KategoriKullanimDTO> kullanim(@PathVariable Long id) {
        return ResponseEntity.ok(kategoriService.kullanim(id));
    }

    @PostMapping
    @Operation(summary = "Yeni kategori oluştur")
    public ResponseEntity<KategoriDTO> olustur(
            @RequestBody @jakarta.validation.Valid KategoriDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(kategoriService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Kategori güncelle", description = "Ad değişimi legacy kolonlara yayılır")
    public ResponseEntity<KategoriDTO> guncelle(
            @PathVariable Long id, @RequestBody @jakarta.validation.Valid KategoriDTO dto) {
        return ResponseEntity.ok(kategoriService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kategori sil", description = "Yalnızca kullanımda değilse (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        kategoriService.sil(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/arsivle")
    @Operation(summary = "Kategoriyi arşivle", description = "aktif=false (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KategoriDTO> arsivle(@PathVariable Long id) {
        return ResponseEntity.ok(kategoriService.arsivle(id));
    }

    @PostMapping("/{id}/aktiflestir")
    @Operation(summary = "Kategoriyi aktifleştir", description = "aktif=true (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KategoriDTO> aktiflestir(@PathVariable Long id) {
        return ResponseEntity.ok(kategoriService.aktiflestir(id));
    }

    @PostMapping("/birlestir")
    @Operation(summary = "Kategorileri birleştir", description = "Kaynak hedefe taşınır ve arşivlenir (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KategoriDTO> birlestir(
            @RequestBody @jakarta.validation.Valid KategoriBirlestirRequest istek) {
        return ResponseEntity.ok(kategoriService.birlestir(istek.getKaynakId(), istek.getHedefId()));
    }

    /** Boş kapsam parametresi "tüm kapsamlar" anlamına gelir. */
    private KategoriKapsam kapsamCoz(String kapsam) {
        if (kapsam == null || kapsam.isBlank()) return null;
        try {
            return KategoriKapsam.valueOf(kapsam.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new com.raspel.erp.exception.BusinessException("Geçersiz kategori kapsamı: " + kapsam);
        }
    }
}
