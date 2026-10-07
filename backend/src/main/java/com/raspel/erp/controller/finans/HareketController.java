package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.service.finans.HareketService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.raspel.erp.util.SayfalamaUtil;
import java.time.LocalDate;
import java.util.List;
import com.raspel.erp.util.CsvGuvenliUtil;
import com.raspel.erp.entity.finans.Hareket;

/**
 * Hareket Controller
 * Hareket işlemleri için REST API endpoint'lerini sağlar.
 */
@Tag(name = "Hareketler", description = "Cari hesap hareketleri API")
@RestController
@RequestMapping("/api/hareketler")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
public class HareketController {

    /** CSV dışa aktarımda bellek koruması için üst sınır. */
    private static final int MAX_CSV_ROWS = 10000;

    private final HareketService hareketService;
    
    @GetMapping("/cari/{cariHesapId}")
    @Operation(summary = "Cari hesap hareketlerini getir",
            description = "Sayfalama için page/size gönderilebilir; gönderilmezse tam liste döner")
    public ResponseEntity<?> cariHesapHareketleriGetir(
            @PathVariable Long cariHesapId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        log.info("GET /api/hareketler/cari/{} - Cari hesap hareketleri getiriliyor", cariHesapId);
        if (SayfalamaUtil.sayfaliMi(page, size)) {
            return ResponseEntity.ok(hareketService.cariHesapHareketleriGetir(cariHesapId,
                    SayfalamaUtil.coz(page, size, Sort.by(Sort.Direction.DESC, "hareketTarihi"))));
        }
        List<HareketDTO> hareketler = hareketService.cariHesapHareketleriGetir(cariHesapId);
        return ResponseEntity.ok(hareketler);
    }
    
    @GetMapping("/son/{limit}")
    @Operation(summary = "Son hareketleri getir", description = "Son n hareketi getirir (Dashboard için)")
    public ResponseEntity<List<HareketDTO>> sonHareketleriGetir(@PathVariable int limit, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("GET /api/hareketler/son/{} - Son {} hareket getiriliyor, sirketId: {}", limit, limit, sirketId);
        List<HareketDTO> hareketler = hareketService.sonHareketleriGetir(limit, sirketId);
        return ResponseEntity.ok(hareketler);
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Hareketleri CSV dışa aktar", description = "Hareketleri CSV dosyası olarak dışa aktarır")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'FINANS_EXPORT')")
    public ResponseEntity<byte[]> hareketlerCsv(
            @RequestParam(required = false) Long cariHesapId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("GET /api/hareketler/export/csv - CSV dışa aktarım, sirketId: {}, cariId: {}, tarih: {}-{}",
                sirketId, cariHesapId, baslangic, bitis);
        // Faz 1.7: dışa aktarım artık liste ekranıyla aynı filtreleri uygular.
        org.springframework.data.domain.Pageable sayfa =
                org.springframework.data.domain.PageRequest.of(0, MAX_CSV_ROWS,
                        Sort.by(Sort.Direction.DESC, "hareketTarihi"));
        List<HareketDTO> liste = (cariHesapId != null || baslangic != null || bitis != null)
                ? hareketService.hareketleriFiltrele(cariHesapId, baslangic, bitis, sayfa, sirketId).getContent()
                : hareketService.tumHareketleriGetir(sirketId, sayfa).getContent();

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Cari Hesap,Tür,Tutar,Tarih,Açıklama\n");
        for (HareketDTO h : liste) {
            csv.append(h.getId()).append(",")
               .append("\"").append(CsvGuvenliUtil.deger(h.getCariHesapAd())).append("\",")
               .append(h.getTur()).append(",")
               .append(h.getTutar()).append(",")
               .append(h.getHareketTarihi() != null ? h.getHareketTarihi() : "").append(",")
               .append("\"").append(CsvGuvenliUtil.deger(h.getAciklama())).append("\"\n");
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("hareketler.csv").build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }


    record AcilisIstek(Long cariHesapId, java.math.BigDecimal tutar, LocalDate tarih, String aciklama) {}

    @PostMapping("/acilis")
    @Operation(summary = "Açılış fişi / devir kaydı",
            description = "Cari için açılış/devir hareketi oluşturur. tutar > 0: cari bize borçlu; tutar < 0: biz cariye borçluyuz")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<HareketDTO> acilis(@RequestBody AcilisIstek istek, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(
                hareketService.acilisKaydet(istek.cariHesapId(), istek.tutar(), istek.tarih(), istek.aciklama(), sirketId));
    }

    @GetMapping
    @Operation(summary = "Tüm hareketleri getir/filtrele", description = "Tüm hareketleri getirir veya filtreleme yapar")
    public ResponseEntity<Page<HareketDTO>> tumHareketleriGetir(
            @RequestParam(required = false) Long cariHesapId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            HttpServletRequest request,
            @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        if (cariHesapId != null || baslangic != null || bitis != null) {
            log.info("GET /api/hareketler - Filtreleme: cariId={}, tarih={}-{}", cariHesapId, baslangic, bitis);
            Page<HareketDTO> hareketler = hareketService.hareketleriFiltrele(cariHesapId, baslangic, bitis, pageable, sirketId);
            return ResponseEntity.ok(hareketler);
        }
        log.info("GET /api/hareketler - Tüm hareketler getiriliyor, sirketId: {}", sirketId);
        Page<HareketDTO> hareketler = hareketService.tumHareketleriGetir(sirketId, pageable);
        return ResponseEntity.ok(hareketler);
    }

    @PostMapping
    @Operation(summary = "Yeni hareket oluştur", description = "Cari hesaba yeni bir hareket (tahsilat/ödeme) oluşturur")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<HareketDTO> hareketOlustur(@RequestBody @jakarta.validation.Valid HareketDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("POST /api/hareketler - Yeni hareket oluşturuluyor, sirketId: {}", sirketId);
        HareketDTO olusturulanHareket = hareketService.hareketOlustur(dto, sirketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(olusturulanHareket);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Hareket güncelle", description = "Hareket bilgilerini günceller")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<HareketDTO> hareketGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid HareketDTO dto) {
        log.info("PUT /api/hareketler/{} - Hareket güncelleniyor", id);
        HareketDTO guncellenen = hareketService.hareketGuncelle(id, dto);
        return ResponseEntity.ok(guncellenen);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hareket sil", description = "Hareketi siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hareketSil(@PathVariable Long id) {
        log.info("DELETE /api/hareketler/{} - Hareket siliniliyor", id);
        hareketService.hareketSil(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/iptal")
    @Operation(summary = "Hareket iptal (soft)",
            description = "Hareketi silmeden iptal eder; bakiye/fatura/kasa-banka etkileri geri alınır, kayıt denetim için saklanır")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<HareketDTO> hareketIptal(@PathVariable Long id) {
        log.info("POST /api/hareketler/{}/iptal - Hareket iptal ediliyor", id);
        return ResponseEntity.ok(hareketService.hareketIptal(id));
    }
}