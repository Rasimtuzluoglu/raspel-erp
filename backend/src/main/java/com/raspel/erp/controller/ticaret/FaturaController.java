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
import org.springframework.scheduling.annotation.Scheduled;
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
    private final com.raspel.erp.service.ticaret.FaturaGecmisService faturaGecmisService;
    private final com.raspel.erp.service.sistem.IdempotencyService idempotencyService;

    @GetMapping
    @Operation(summary = "Tüm faturaları getir (sayfalı)", description = "Şirkete ait tüm faturaları sayfalı olarak listeler. search ile fatura no/cari adı araması yapılır.")
    public ResponseEntity<Page<FaturaDTO>> tumFaturalariGetir(
            HttpServletRequest request,
            @PageableDefault(size = 50) Pageable pageable,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "bas", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bas,
            @RequestParam(value = "bit", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bit) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        if ((search != null && !search.isBlank()) || bas != null || bit != null) {
            return ResponseEntity.ok(yazdirmaOzetiEkle(faturaService.ara(sirketId, search, bas, bit, pageable)));
        }
        return ResponseEntity.ok(yazdirmaOzetiEkle(faturaService.tumFaturalariGetir(sirketId, pageable)));
    }

    private Page<FaturaDTO> yazdirmaOzetiEkle(Page<FaturaDTO> sayfa) {
        java.util.List<Long> ids = sayfa.getContent().stream().map(FaturaDTO::getId).toList();
        java.util.Map<Long, com.raspel.erp.dto.ticaret.FaturaYazdirmaOzetDTO> ozet =
                faturaGecmisService.yazdirmaOzetleri(ids);
        if (ozet == null) return sayfa;
        for (FaturaDTO f : sayfa.getContent()) {
            com.raspel.erp.dto.ticaret.FaturaYazdirmaOzetDTO o = ozet.get(f.getId());
            if (o != null) {
                f.setYazdirmaSayisi(o.getAdet());
                f.setSonYazdirmaTarihi(o.getSonTarih());
                f.setSonYazdirmaFormat(o.getSonFormat());
                f.setSonYazdirmaYazici(o.getSonYazici());
            }
        }
        return sayfa;
    }

    @GetMapping("/numara/{faturaNumarasi}")
    @Operation(summary = "Fatura numarasına göre getir",
            description = "Benzersiz fatura numarasından faturayı getirir (fişteki numaradan kayda ulaşma)")
    public ResponseEntity<FaturaDTO> faturaNumarasiIleGetir(@PathVariable String faturaNumarasi,
                                                            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(faturaService.faturaNumarasiIleGetir(faturaNumarasi, sirketId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre fatura getir", description = "Fatura ID'sine göre detayları getirir")
    public ResponseEntity<FaturaDTO> faturaGetir(@PathVariable Long id) {
        return ResponseEntity.ok(faturaService.faturaGetir(id));
    }

    @GetMapping("/{id}/para-izi")
    @Operation(summary = "Faturanın para izi",
            description = "Faturaya bağlı kasa/banka hareketlerini, iadeleri ve irsaliye bağını getirir")
    public ResponseEntity<com.raspel.erp.dto.ticaret.FaturaParaIziDTO> faturaParaIzi(@PathVariable Long id) {
        return ResponseEntity.ok(faturaService.faturaParaIzi(id));
    }

    @GetMapping("/stok/{stokId}/fiyat-gecmisi")
    @Operation(summary = "Stok fiyat gecmisi", description = "Bir stogun son 5 alis fiyatini ve trend yonunu dondurur")
    public ResponseEntity<com.raspel.erp.dto.envanter.StokFiyatGecmisiDTO> stokFiyatGecmisi(
            @PathVariable Long stokId,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(faturaService.stokFiyatGecmisi(stokId, sirketId));
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

    @GetMapping("/cari/{cariId}")
    @Operation(summary = "Carinin faturalari", description = "Cari hesaba ait tüm faturaları (fişleri) sayfalı olarak listeler")
    public ResponseEntity<Page<FaturaDTO>> cariFaturalari(
            @PathVariable Long cariId,
            HttpServletRequest request,
            @PageableDefault(size = 20) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(faturaService.cariFaturalari(cariId, sirketId, pageable));
    }

    @GetMapping("/cari/{cariId}/stok/{stokId}/fiyat-gecmisi")
    @Operation(summary = "Cari-urun fiyat gecmisi", description = "Bir cari hesabin belirli bir urunu gecmiste aldıgı fiyatları dondurur")
    public ResponseEntity<com.raspel.erp.dto.ticaret.CariUrunFiyatDTO> cariUrunFiyatGecmisi(
            @PathVariable Long cariId,
            @PathVariable Long stokId,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(faturaService.cariUrunFiyatGecmisi(cariId, stokId, sirketId));
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
            Long sirketId = (Long) request.getAttribute("sirketId");
            Long kullaniciId = (Long) request.getAttribute("kullaniciId");
            String displayName = (String) request.getAttribute("displayName");

            // Dağıtık kilit: anahtarı fatura oluşturmadan ÖNCE rezerve et.
            // Aynı isteğin tekrarı (retry) mükerrer fatura üretmez.
            String anahtar = "idem:fatura:" + (sirketId != null ? sirketId : 0L) + ":" + idempotencyKey;
            if (!idempotencyService.deneKilit(anahtar)) {
                // Başka bir istek işliyor olabilir; kısa süre sonucu bekle.
                for (int i = 0; i < 20; i++) {
                    java.util.Optional<Long> sonuc = idempotencyService.tamamlananSonuc(anahtar);
                    if (sonuc.isPresent()) {
                        return ResponseEntity.ok(faturaService.faturaGetir(sonuc.get()));
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                throw new com.raspel.erp.exception.BusinessException(
                        "Bu istek hâlâ işleniyor. Lütfen birkaç saniye sonra tekrar deneyin.");
            }

            try {
                FaturaDTO olusturulan = faturaOlusturTekrarDene(dto, sirketId, kullaniciId, displayName);
                idempotencyService.tamamla(anahtar, olusturulan.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(olusturulan);
            } catch (RuntimeException e) {
                // Oluşturma başarısızsa kilidi bırak; sonraki deneme çalışsın.
                idempotencyService.serbestBirak(anahtar);
                throw e;
            }
        }
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String displayName = (String) request.getAttribute("displayName");
        FaturaDTO olusturulan = faturaOlusturTekrarDene(dto, sirketId, kullaniciId, displayName);
        return ResponseEntity.status(HttpStatus.CREATED).body(olusturulan);
    }

    /**
     * Fatura numarası uygulama tarafında (max+1) üretildiği için eşzamanlı istekler
     * aynı numarayı üretip `uk_fatura_no_sirket` ihlali verebilir. Bu durumda işlem
     * (yeni numara ile) birkaç kez yeniden denenir; kullanıcı hata görmez.
     */
    private FaturaDTO faturaOlusturTekrarDene(FaturaDTO dto, Long sirketId, Long kullaniciId, String displayName) {
        return com.raspel.erp.support.MukerrerKayitRetry.calistir(
                () -> faturaService.faturaOlustur(dto, sirketId, kullaniciId, displayName),
                e -> com.raspel.erp.support.MukerrerKayitRetry.kisitMi(e, "uk_fatura_no_sirket"));
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

    @PostMapping("/{id}/yeniden-hesapla")
    @Operation(summary = "Faturayı yeniden hesapla",
            description = "KDV dahil modele göre ara toplam/KDV/genel toplamı yeniden üretir. " +
                    "kaydet=false ise yalnızca önizleme (dry-run). Yalnızca ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FaturaDTO> yenidenHesapla(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "false") boolean kaydet) {
        return ResponseEntity.ok(faturaService.faturaYenidenHesapla(id, kaydet));
    }

    @GetMapping("/yeniden-hesapla/toplu")
    @Operation(summary = "Geçmiş faturaları toplu yeniden hesapla (önizleme)",
            description = "KDV-dahil modele göre tüm eşleşen faturaları tarar ve özet döner (dry-run). " +
                    "Hiçbir kayıt değişmez. Yalnızca ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.raspel.erp.dto.ticaret.FaturaTopluHesaplaDTO> topluYenidenHesaplaOnizleme(
            HttpServletRequest request,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bas,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bit,
            @RequestParam(required = false) String tur) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(faturaService.faturaTopluYenidenHesapla(sirketId, bas, bit, tur, false));
    }

    @PostMapping("/yeniden-hesapla/toplu")
    @Operation(summary = "Geçmiş faturaları toplu yeniden hesapla (uygula)",
            description = "Değişen ve dönemi kilitli olmayan faturaları KDV-dahil modele göre günceller; " +
                    "her biri için geçmiş kaydı alınır. Yalnızca ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.raspel.erp.dto.ticaret.FaturaTopluHesaplaDTO> topluYenidenHesaplaUygula(
            HttpServletRequest request,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bas,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bit,
            @RequestParam(required = false) String tur) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(faturaService.faturaTopluYenidenHesapla(sirketId, bas, bit, tur, true));
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

    @GetMapping("/{id}/gecmis")
    @Operation(summary = "Fatura işlem geçmişi", description = "Oluşturma/düzenleme/durum/silme/yazdırma olaylarını zaman çizelgesi olarak döndürür")
    public ResponseEntity<List<com.raspel.erp.dto.ticaret.FaturaGecmisDTO>> faturaGecmis(@PathVariable Long id) {
        return ResponseEntity.ok(faturaGecmisService.gecmis(id));
    }

    @PostMapping("/{id}/yazdirma")
    @Operation(summary = "Fatura yazdırma kaydı", description = "Faturanın yazdırıldığını kaydeder (biçim ve varsa yazıcı adı ile)")
    public ResponseEntity<com.raspel.erp.dto.ticaret.FaturaGecmisDTO> yazdirmaKaydet(
            @PathVariable Long id,
            @RequestBody(required = false) YazdirmaRequest body) {
        String format = body != null ? body.format() : null;
        String yazici = body != null ? body.yaziciAdi() : null;
        return ResponseEntity.ok(faturaGecmisService.yazdirmaKaydet(id, format, yazici));
    }

    record YazdirmaRequest(String format, String yaziciAdi) {}

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
}