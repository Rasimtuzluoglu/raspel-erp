package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.FaturaDTO;
import com.raspel.erp.service.ticaret.FaturaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.raspel.erp.entity.ticaret.Fatura;

@Tag(name = "Faturalar", description = "Fatura yönetimi API")
@RestController
@RequestMapping("/api/faturalar")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class FaturaController {

    private final FaturaService faturaService;
    private final java.util.concurrent.ConcurrentHashMap<String, IdempotencyKaydi> idempotencyCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long IDEMPOTENCY_TTL_MS = 10 * 60 * 1000;

    @GetMapping
    @Operation(summary = "Tüm faturaları getir (sayfalı)", description = "Şirkete ait tüm faturaları sayfalı olarak listeler")
    public ResponseEntity<Page<FaturaDTO>> tumFaturalariGetir(
            HttpServletRequest request,
            @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(faturaService.tumFaturalariGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre fatura getir", description = "Fatura ID'sine göre detayları getirir")
    public ResponseEntity<FaturaDTO> faturaGetir(@PathVariable Long id) {
        return ResponseEntity.ok(faturaService.faturaGetir(id));
    }

    @GetMapping("/cari/{cariId}/son-urunler")
    @Operation(summary = "Carinin son aldigi urunler", description = "Cari hesabin son aldigi urunleri listeler (fatura olustururken onerilen urunler)")
    public ResponseEntity<List<com.raspel.erp.dto.ticaret.CariSonUrunDTO>> cariSonUrunler(
            @PathVariable Long cariId,
            HttpServletRequest request,
            @RequestParam(defaultValue = "10") int limit) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(faturaService.cariSonUrunler(cariId, sirketId, limit));
    }

    @GetMapping("/cari/{cariId}/son-fatura")
    @Operation(summary = "Carinin son faturasi", description = "Cari hesabin son faturasini dondurur (kopyalama icin)")
    public ResponseEntity<FaturaDTO> cariSonFatura(
            @PathVariable Long cariId,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        FaturaDTO son = faturaService.cariSonFatura(cariId, sirketId);
        if (son == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(son);
    }

    @PostMapping
    @Operation(summary = "Yeni fatura oluştur", description = "Yeni bir fatura oluşturur. X-Idempotency-Key header ile çift kayıt engellenir.")
    public ResponseEntity<FaturaDTO> faturaOlustur(
            @RequestBody @jakarta.validation.Valid FaturaDTO dto,
            HttpServletRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null) {
            IdempotencyKaydi mevcut = idempotencyCache.get(idempotencyKey);
            if (mevcut != null && !mevcut.suresiDoldu()) {
                return ResponseEntity.ok(mevcut.fatura);
            }
            Long sirketId = (Long) request.getAttribute("sirketId");
            Long kullaniciId = (Long) request.getAttribute("kullaniciId");
            String displayName = (String) request.getAttribute("displayName");
            FaturaDTO olusturulan = faturaService.faturaOlustur(dto, sirketId, kullaniciId, displayName);
            IdempotencyKaydi yeni = new IdempotencyKaydi(olusturulan);
            IdempotencyKaydi onceki = idempotencyCache.putIfAbsent(idempotencyKey, yeni);
            if (onceki != null && !onceki.suresiDoldu()) {
                return ResponseEntity.ok(onceki.fatura);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(olusturulan);
        }
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String displayName = (String) request.getAttribute("displayName");
        FaturaDTO olusturulan = faturaService.faturaOlustur(dto, sirketId, kullaniciId, displayName);
        return ResponseEntity.status(HttpStatus.CREATED).body(olusturulan);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Fatura güncelle", description = "Fatura bilgilerini günceller")
    public ResponseEntity<FaturaDTO> faturaGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid FaturaDTO dto) {
        return ResponseEntity.ok(faturaService.faturaGuncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    @Operation(summary = "Fatura durum güncelle", description = "Fatura durumunu günceller (ödendi/bekliyor/iptal)")
    public ResponseEntity<FaturaDTO> faturaDurumGuncelle(@PathVariable Long id, @Valid @RequestBody DurumRequest request) {
        return ResponseEntity.ok(faturaService.faturaDurumGuncelle(id, request.durum));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Fatura sil", description = "Faturayı siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> faturaSil(@PathVariable Long id) {
        faturaService.faturaSil(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/gonder-email")
    @Operation(summary = "Faturayı e-posta ile gönder", description = "Fatura PDF'ini cari hesabın e-posta adresine gönderir")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> gonderEmail(@PathVariable Long id) {
        faturaService.gonderEmail(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Faturaları CSV dışa aktar", description = "Faturaları CSV dosyası olarak dışa aktarır")
    public ResponseEntity<byte[]> exportCsv(HttpServletRequest request) {
        List<FaturaDTO> liste = faturaService.tumFaturalariGetir((Long) request.getAttribute("sirketId"), PageRequest.of(0, 10000)).getContent();
        StringBuilder csv = new StringBuilder("Fatura No,Tarih,Müşteri,Tutar,Durum\n");
        for (FaturaDTO f : liste) {
            csv.append(csvSafe(f.getFaturaNumarasi())).append(",")
               .append(f.getTarih()).append(",")
               .append(csvSafe(f.getCariHesapAd()))
               .append(",").append(f.getGenelToplam())
               .append(",").append(csvSafe(f.getDurum())).append("\n");
        }
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("faturalar.csv").build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private String csvSafe(String deger) {
        if (deger == null || deger.isBlank()) return "\"\"";
        String s = deger.trim();
        if (s.startsWith("=") || s.startsWith("+") || s.startsWith("-") || s.startsWith("@")) {
            s = "'" + s;
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    record DurumRequest(String durum) {}

    private static final class IdempotencyKaydi {
        final FaturaDTO fatura;
        final long olusturmaZamani;

        IdempotencyKaydi(FaturaDTO fatura) {
            this.fatura = fatura;
            this.olusturmaZamani = System.currentTimeMillis();
        }

        boolean suresiDoldu() {
            return System.currentTimeMillis() - olusturmaZamani > IDEMPOTENCY_TTL_MS;
        }
    }
}