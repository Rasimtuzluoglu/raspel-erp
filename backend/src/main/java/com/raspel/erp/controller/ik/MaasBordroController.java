package com.raspel.erp.controller.ik;

import com.raspel.erp.dto.ik.MaasBordroDTO;
import com.raspel.erp.service.ik.MaasBordroService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@Tag(name = "Maaş Bordro", description = "Maaş bordro yönetimi API")
@RestController
@RequestMapping("/api/maas-bordro")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
public class MaasBordroController {

    private final MaasBordroService maasBordroService;
    private final com.raspel.erp.service.ik.BordroHesaplamaService bordroHesaplamaService;

    @GetMapping("/ayar")
    @Operation(summary = "Bordro hesaplama ayarlarını getir",
            description = "Yıl bazlı asgari ücret, SGK/işsizlik, damga ve gelir vergisi dilimlerini getirir (yoksa varsayılan oluşturulur)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<com.raspel.erp.entity.ik.BordroAyar> ayar(
            HttpServletRequest request,
            @RequestParam(required = false) Integer yil) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(bordroHesaplamaService.ayarGetirVeyaOlustur(sirketId, yil));
    }

    @PutMapping("/ayar")
    @Operation(summary = "Bordro hesaplama ayarlarını kaydet",
            description = "Asgari ücret, SGK/işsizlik/damga oranları ve gelir vergisi dilimlerini kaydeder")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<com.raspel.erp.entity.ik.BordroAyar> ayarKaydet(
            @RequestBody com.raspel.erp.entity.ik.BordroAyar dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(bordroHesaplamaService.ayarKaydet(sirketId, dto));
    }

    @PostMapping("/hesapla")
    @Operation(summary = "Bordro hesapla (kaydetmeden)",
            description = "Brüt maaştan SGK, gelir ve damga vergisi kesintileri ile net ve işveren maliyetini hesaplar")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<com.raspel.erp.dto.ik.BordroHesaplamaDTO> hesapla(
            @RequestBody com.raspel.erp.dto.ik.BordroHesaplamaDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(bordroHesaplamaService.hesapla(dto, sirketId));
    }

    @PostMapping("/toplu-uret")
    @Operation(summary = "Toplu bordro üret",
            description = "Aktif personel için verilen ayın TASLAK bordrolarını hesaplayıp üretir; mevcut kayıtlar atlanır. personelIds verilirse yalnız seçilenler üretilir")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<java.util.Map<String, Object>> topluUret(
            HttpServletRequest request,
            @RequestParam Integer yil,
            @RequestParam Integer ay,
            @RequestBody(required = false) java.util.List<Long> personelIds) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(bordroHesaplamaService.topluUret(sirketId, yil, ay, personelIds));
    }

    @GetMapping("/toplu-onizleme")
    @Operation(summary = "Toplu bordro önizlemesi",
            description = "Verilen ay için hangi personelin üretileceğini/atlanacağını ve tahmini neti döndürür")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<java.util.List<com.raspel.erp.dto.ik.BordroOnizlemeDTO>> topluOnizleme(
            HttpServletRequest request,
            @RequestParam Integer yil,
            @RequestParam Integer ay) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(bordroHesaplamaService.topluOnizleme(sirketId, yil, ay));
    }

    @GetMapping
    @Operation(summary = "Tüm maaş bordrolarını getir",
            description = "Bordro kayıtlarını personel/dönem/durum filtreleriyle sayfalı listeler")
    public ResponseEntity<Page<MaasBordroDTO>> tumu(
            HttpServletRequest request,
            @RequestParam(required = false) Integer yil,
            @RequestParam(required = false) Integer ay,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) String odemeDurumu,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 25) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(maasBordroService.tumunuGetir(sirketId, yil, ay, durum, odemeDurumu, q, pageable));
    }

    @GetMapping("/ozet")
    @Operation(summary = "Bordro KPI özeti", description = "Filtreli toplam brüt/kesinti/net ve ödenen net tutarları")
    public ResponseEntity<java.util.Map<String, Object>> ozet(
            HttpServletRequest request,
            @RequestParam(required = false) Integer yil,
            @RequestParam(required = false) Integer ay,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) String odemeDurumu,
            @RequestParam(required = false) String q) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(maasBordroService.ozet(sirketId, yil, ay, durum, odemeDurumu, q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre maaş bordrosu getir", description = "Maaş bordrosu ID'sine göre detayları getirir")
    public ResponseEntity<MaasBordroDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(maasBordroService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni maaş bordrosu oluştur", description = "Yeni bir maaş bordrosu oluşturur")
    public ResponseEntity<MaasBordroDTO> olustur(@Valid @RequestBody MaasBordroDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(maasBordroService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Maaş bordrosu güncelle", description = "Maaş bordrosu bilgilerini günceller")
    public ResponseEntity<MaasBordroDTO> guncelle(@PathVariable Long id, @Valid @RequestBody MaasBordroDTO dto) {
        return ResponseEntity.ok(maasBordroService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Maaş bordrosu sil", description = "Maaş bordrosunu siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        maasBordroService.sil(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/onayla")
    @Operation(summary = "Bordroyu onayla", description = "Bordroyu onaylar ve kilitler; kasaId verilirse net tutar kasadan ödenir")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<MaasBordroDTO> onayla(@PathVariable Long id,
                                                @RequestParam(required = false) Long kasaId,
                                                HttpServletRequest request) {
        String onaylayan = onaylayanKisi();
        return ResponseEntity.ok(maasBordroService.onayla(id, onaylayan, kasaId));
    }

    /**
     * REDTEAM/Faz1.3: Onaylayan adi dogrulanmis SecurityContext'ten alinir.
     * Eskiden {@code request.getAttribute("username")} okunuyordu; bu nitelik
     * JwtAuthFilter'da hic set edilmedigi icin kayit daima "Yonetici" idi.
     *
     * @return onaylayan kullanici adi
     */
    private String onaylayanKisi() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return (auth != null && auth.getName() != null && !auth.getName().isBlank())
                ? auth.getName()
                : "Bilinmiyor";
    }

    @PostMapping("/{id}/onay-kaldir")
    @Operation(summary = "Bordro onayını kaldır", description = "Onaylanmış bordroyu yeniden düzenlenebilir yapar (ödeme varsa geri alınır)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<MaasBordroDTO> onayKaldir(@PathVariable Long id) {
        return ResponseEntity.ok(maasBordroService.onayKaldir(id));
    }

    @PostMapping("/{id}/ode")
    @Operation(summary = "Bordroyu öde", description = "Onaylanmış bordronun net tutarını seçilen kasadan öder")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<MaasBordroDTO> ode(@PathVariable Long id, @RequestParam Long kasaId) {
        return ResponseEntity.ok(maasBordroService.ode(id, kasaId));
    }
}
