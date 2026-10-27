package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.ButceDTO;
import com.raspel.erp.service.finans.ButceService;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import java.util.List;
import java.util.Locale;

@Tag(name = "Bütçeler", description = "Bütçe yönetimi API")
@RestController
@RequestMapping("/api/butceler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
public class ButceController {

    private final ButceService butceService;
    private final com.raspel.erp.service.sistem.RaporService raporService;

    @GetMapping
    @Operation(summary = "Tüm bütçeleri getir", description = "Tüm bütçe kayıtlarını listeler")
    public ResponseEntity<Page<ButceDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(butceService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/filtreli")
    @Operation(summary = "Bütçeleri filtreli ve sayfalı getir",
            description = "Yıl, ay, tür, kategori ve serbest metin filtreleriyle sunucu taraflı listeler")
    public ResponseEntity<Page<ButceDTO>> filtreli(
            @RequestParam(required = false) Integer yil,
            @RequestParam(required = false) Integer ay,
            @RequestParam(required = false) String tur,
            @RequestParam(required = false) String kategori,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 25, sort = "yil", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(butceService.filtreli(sirketId, yil, ay, tur, kategori, durum, q, pageable));
    }

    @GetMapping("/ozet")
    @Operation(summary = "Bütçe KPI özeti",
            description = "Yıl ve tür bazlı bütçe, gerçekleşen, sapma ve kullanım yüzdesi")
    public ResponseEntity<com.raspel.erp.dto.finans.ButceOzetDTO> ozet(
            @RequestParam(required = false) Integer yil,
            @RequestParam(required = false) String tur,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(butceService.ozet(sirketId, yil, tur));
    }

    @GetMapping("/onay-bekleyen")
    @Operation(summary = "Onay bekleyen bütçeler", description = "TASLAK durumundaki bütçeleri listeler")
    public ResponseEntity<List<ButceDTO>> onayBekleyen(
            @RequestParam(required = false) Integer yil,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(butceService.onayBekleyenler(sirketId, yil));
    }

    @PutMapping("/{id}/onayla")
    @Operation(summary = "Taslak bütçeyi onayla")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<ButceDTO> onayla(@PathVariable Long id, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(butceService.onayla(id, kullaniciId));
    }

    @PutMapping("/{id}/reddet")
    @Operation(summary = "Taslak bütçeyi reddet")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<ButceDTO> reddet(
            @PathVariable Long id,
            @RequestBody(required = false) RedIstek istek,
            HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(butceService.reddet(id, istek != null ? istek.gerekce() : null, kullaniciId));
    }

    @GetMapping("/{id}/revizyonlar")
    @Operation(summary = "Bütçe revizyon geçmişi")
    public ResponseEntity<List<com.raspel.erp.dto.finans.ButceRevizyonDTO>> revizyonlar(@PathVariable Long id) {
        return ResponseEntity.ok(butceService.revizyonlar(id));
    }

    @GetMapping("/kategoriler")    @Operation(summary = "Bütçe kategorilerini getir")
    public ResponseEntity<List<String>> kategoriler(
            @RequestParam(required = false) Integer yil,
            @RequestParam(required = false) String tur,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(butceService.kategoriler(sirketId, yil, tur));
    }

    @GetMapping("/matris")
    @Operation(summary = "12 aylık bütçe matrisi", description = "Kategori × ay bütçe dağılımı ve toplamlar")
    public ResponseEntity<com.raspel.erp.dto.finans.ButceMatrisDTO> matris(
            @RequestParam(required = false) Integer yil,
            @RequestParam(required = false) String tur,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(butceService.matris(sirketId, yil, tur));
    }

    @PostMapping("/kopyala")
    @Operation(summary = "Önceki yıldan bütçe kopyala",
            description = "Kaynak yılın bütçelerini hedef yıla kopyalar; mevcut kategori+ay+tür atlanır")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<java.util.Map<String, Integer>> kopyala(
            @RequestBody KopyalaIstek istek,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(butceService.kopyala(
                sirketId, istek.kaynakYil(), istek.hedefYil(), istek.tur()));
    }

    @GetMapping("/gerceklesen")
    @Operation(summary = "Tür ayrımlı bütçe–gerçekleşen raporu",            description = "Gider bütçesi masraflarla, gelir bütçesi tahsilat hareketleriyle karşılaştırılır")
    public ResponseEntity<com.raspel.erp.dto.finans.ButceGerceklesenRaporDTO> gerceklesen(
            @RequestParam(required = false) Integer yil,
            @RequestParam(required = false) Integer ay,
            @RequestParam(required = false) String tur,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(butceService.gerceklesen(sirketId, yil, ay, tur));
    }

    @GetMapping("/gerceklesen/pdf")
    @Operation(summary = "Bütçe–gerçekleşen raporunu PDF indir")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'RAPOR_EXPORT')")
    public ResponseEntity<byte[]> gerceklesenPdf(
            @RequestParam(required = false) Integer yil,
            @RequestParam(required = false) Integer ay,
            @RequestParam(required = false) String tur,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var rapor = butceService.gerceklesen(sirketId, yil, ay, tur);
        String[] kolonlar = {"Kategori", "Bütçe", "Gerçekleşen", "Sapma", "Kullanım %"};
        List<String[]> satirlar = rapor.getSatirlar().stream().map(r -> new String[]{
                r.getKategori(),
                r.getButce() != null ? r.getButce().toPlainString() : "-",
                r.getGerceklesen() != null ? r.getGerceklesen().toPlainString() : "-",
                r.getSapma() != null ? r.getSapma().toPlainString() : "-",
                r.getKullanimYuzdesi() != null ? r.getKullanimYuzdesi().toPlainString() : "-"
        }).toList();
        byte[] pdf = raporService.butceGerceklesenPdf(kolonlar, satirlar, rapor.getYil(), rapor.getAy());
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename("butce-gerceklesen-" + rapor.getTur().toLowerCase(Locale.ROOT) + "-"
                        + rapor.getYil() + (rapor.getAy() != null ? "-" + rapor.getAy() : "") + ".pdf")
                .build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre bütçe getir", description = "Bütçe ID'sine göre detayları getirir")
    public ResponseEntity<ButceDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(butceService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni bütçe oluştur", description = "Yeni bir bütçe kaydı oluşturur")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<ButceDTO> olustur(@Valid @RequestBody ButceDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(butceService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Bütçe güncelle", description = "Bütçe bilgilerini günceller")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<ButceDTO> guncelle(@PathVariable Long id, @Valid @RequestBody ButceDTO dto) {
        return ResponseEntity.ok(butceService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Bütçe sil", description = "Bütçeyi siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        butceService.sil(id);
        return ResponseEntity.noContent().build();
    }

    record KopyalaIstek(Integer kaynakYil, Integer hedefYil, String tur) {
    }

    record RedIstek(String gerekce) {
    }
}
