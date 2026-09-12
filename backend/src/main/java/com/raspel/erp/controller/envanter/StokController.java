package com.raspel.erp.controller.envanter;

import com.raspel.erp.dto.envanter.StokDTO;
import com.raspel.erp.dto.envanter.StokHareketDTO;
import com.raspel.erp.dto.envanter.KritikStokDTO;
import com.raspel.erp.service.envanter.StokService;
import com.raspel.erp.service.envanter.StokAnalizService;
import com.raspel.erp.service.sistem.QRService;
import com.raspel.erp.service.sistem.PdfRaporService;
import com.raspel.erp.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.raspel.erp.entity.envanter.Stok;

@Tag(name = "Stoklar", description = "Stok yönetimi API")
@RestController
@RequestMapping("/api/stoklar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class StokController {

    private final StokService stokService;
    private final StokAnalizService stokAnalizService;
    private final QRService qrService;
    private final PdfRaporService pdfRaporService;

    @GetMapping
    @Operation(summary = "Tüm stokları getir (sayfalı)", description = "Tüm stokları sayfalı olarak listeler")
    public ResponseEntity<Page<StokDTO>> tumu(
            HttpServletRequest request,
            @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/barkod/{kod}")
    @Operation(summary = "Barkod ile stok bul", description = "Barkoda göre eşleşen ilk stoğu döndürür (tarama akışları için)")
    public ResponseEntity<StokDTO> barkodIleBul(@PathVariable String kod, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        StokDTO dto = stokService.barkodIleBul(kod, sirketId);
        if (dto == null) {
            throw new ResourceNotFoundException("Barkod eşleşen stok bulunamadı: " + kod);
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/etiket-qr")
    @Operation(summary = "Stok QR kodu (PNG)", description = "Stoğun barkod/kod bilgisini içeren QR kodu görüntüsü üretir")
    public ResponseEntity<byte[]> etiketQr(@PathVariable Long id) {
        Stok stok = stokService.entityGetir(id);
        String icerik = qrIcerik(stok);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(qrService.qrPng(icerik, 220));
    }

    @GetMapping("/{id}/etiket")
    @Operation(summary = "Raf etiketi (PDF)", description = "Ürün adı, kod, barkod, QR, raf no ve fiyat içeren etiket PDF'i üretir")
    public ResponseEntity<byte[]> etiketPdf(@PathVariable Long id) {
        Stok stok = stokService.entityGetir(id);
        byte[] qr = qrService.qrPng(qrIcerik(stok), 200);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=etiket-" + stok.getId() + ".pdf")
                .body(pdfRaporService.stokEtiketi(stok, qr));
    }

    private String qrIcerik(Stok stok) {
        if (stok.getBarkod() != null && !stok.getBarkod().isBlank()) return stok.getBarkod();
        if (stok.getStokKodu() != null && !stok.getStokKodu().isBlank()) return stok.getStokKodu();
        return "STK" + stok.getId();
    }

    @GetMapping("/filtreli")
    @Operation(summary = "Stokları filtrele (sayfalı)", description = "Arama, kategori, marka, stok grubu ve fiyat aralığına göre sunucu tarafında filtreler")
    public ResponseEntity<Page<StokDTO>> filtreli(
            HttpServletRequest request,
            @PageableDefault(size = 25) Pageable pageable,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String kategori,
            @RequestParam(required = false) String marka,
            @RequestParam(required = false) String stokGrubu,
            @RequestParam(required = false) java.math.BigDecimal minFiyat,
            @RequestParam(required = false) java.math.BigDecimal maxFiyat) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokService.filtreli(sirketId, q, kategori, marka, stokGrubu, minFiyat, maxFiyat, pageable));
    }

    @GetMapping("/ara")
    @Operation(summary = "Stok ara", description = "Stokları ada/barkoda göre arar")
    public ResponseEntity<List<StokDTO>> ara(@RequestParam String q, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokService.ara(q, sirketId));
    }

    @GetMapping("/en-cok-satanlar")
    @Operation(summary = "En çok satanlar", description = "Satış miktarına göre en çok satan ürünleri tam detaylarıyla getirir (POS hızlı erişim)")
    public ResponseEntity<List<StokDTO>> enCokSatanlar(
            HttpServletRequest request,
            @RequestParam(defaultValue = "12") int limit) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokService.enCokSatanlar(sirketId, limit));
    }

    @GetMapping("/kritik")
    @Operation(summary = "Kritik stoklar", description = "Kritik seviyeye düşen stokları ve önerilen sipariş miktarlarını listeler")
    public ResponseEntity<List<KritikStokDTO>> kritikStoklar(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokService.kritikStoklar(sirketId));
    }

    @GetMapping("/talep-tahmini")
    @Operation(summary = "Akıllı talep tahmini", description = "Tüketim trendlerine göre tahmini tükenme günü ve proaktif sipariş önerilerini getirir")
    public ResponseEntity<List<com.raspel.erp.dto.envanter.TalepTahminiDTO>> talepTahmini(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokService.talepTahmini(sirketId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre stok getir", description = "Stok ID'sine göre detayları getirir")
    public ResponseEntity<StokDTO> getir(@PathVariable Long id) { return ResponseEntity.ok(stokService.getir(id)); }

    // ÇOKLU FİYAT

    @GetMapping("/{id}/fiyatlar")
    @Operation(summary = "Stok fiyatlarını getir", description = "Bir stoğa ait tüm fiyat tanımlarını listeler")
    public ResponseEntity<List<com.raspel.erp.dto.envanter.StokFiyatDTO>> fiyatlar(@PathVariable Long id) {
        return ResponseEntity.ok(stokService.fiyatlariGetir(id));
    }

    @PostMapping("/{id}/fiyatlar")
    @Operation(summary = "Stok fiyatı ekle", description = "Stoğa yeni bir fiyat tanımı ekler")
    public ResponseEntity<com.raspel.erp.dto.envanter.StokFiyatDTO> fiyatEkle(
            @PathVariable Long id,
            @RequestBody com.raspel.erp.dto.envanter.StokFiyatDTO dto,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(stokService.fiyatEkle(id, dto, sirketId));
    }

    @PutMapping("/fiyatlar/{fiyatId}")
    @Operation(summary = "Stok fiyatı güncelle", description = "Fiyat tanımını günceller")
    public ResponseEntity<com.raspel.erp.dto.envanter.StokFiyatDTO> fiyatGuncelle(
            @PathVariable Long fiyatId,
            @RequestBody com.raspel.erp.dto.envanter.StokFiyatDTO dto) {
        return ResponseEntity.ok(stokService.fiyatGuncelle(fiyatId, dto));
    }

    @DeleteMapping("/fiyatlar/{fiyatId}")
    @Operation(summary = "Stok fiyatı sil", description = "Fiyat tanımını siler")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> fiyatSil(@PathVariable Long fiyatId) {
        stokService.fiyatSil(fiyatId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Operation(summary = "Yeni stok oluştur", description = "Yeni bir stok/ürün oluşturur")
    public ResponseEntity<StokDTO> olustur(@RequestBody @jakarta.validation.Valid StokDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(stokService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Stok güncelle", description = "Stok bilgilerini günceller")
    public ResponseEntity<StokDTO> guncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid StokDTO dto) {
        return ResponseEntity.ok(stokService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Stok sil", description = "Stoku siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) { stokService.sil(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/{id}/hareketler")
    @Operation(summary = "Stok hareketlerini getir", description = "Belirli bir stoğa ait hareketleri listeler")
    public ResponseEntity<List<StokHareketDTO>> hareketler(@PathVariable Long id) {
        return ResponseEntity.ok(stokService.hareketler(id));
    }

    @GetMapping("/hareketler/tum")
    @Operation(summary = "Tüm stok hareketlerini getir", description = "Tüm stok hareketlerini listeler")
    public ResponseEntity<List<StokHareketDTO>> tumHareketler(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokService.tumHareketler(sirketId));
    }

    @PostMapping("/{id}/hareketler")
    @Operation(summary = "Stok hareketi ekle", description = "Stoka yeni bir giriş/çıkış hareketi ekler")
    public ResponseEntity<StokHareketDTO> hareketEkle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid StokHareketDTO dto) {
        dto.setStokId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(stokService.hareketEkle(dto));
    }

    @PostMapping("/toplu-fiyat-guncelle")
    @Operation(summary = "Toplu stok fiyatı güncelle", description = "Seçilen kategori/gruba göre stok fiyatlarını yüzde ile artırır/azaltır")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> topluFiyatGuncelle(
            @RequestBody @jakarta.validation.Valid com.raspel.erp.dto.envanter.TopluFiyatDTO dto,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        int etkilenen = stokService.topluFiyatGuncelle(dto, sirketId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "etkilenenStokSayisi", etkilenen,
                "mesaj", etkilenen + " adet stok fiyatı başarıyla güncellendi."
        ));
    }

    @DeleteMapping("/hareketler/{hareketId}")
    @Operation(summary = "Stok hareketi sil", description = "Stok hareketini siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hareketSil(@PathVariable Long hareketId) {
        stokService.hareketSil(hareketId);
        return ResponseEntity.noContent().build();
    }

    // ---------- ÜRÜN ANALİZİ (maliyet / alış-satış / kârlılık) ----------

    @GetMapping("/{id}/analiz")
    @Operation(summary = "Stok analiz özeti", description = "Alış-satış özeti ve kârlılık bilgisini tek yanıtta döndürür (tarih aralıklı)")
    public ResponseEntity<com.raspel.erp.dto.envanter.StokAnalizDTO> analiz(
            @PathVariable Long id, HttpServletRequest request,
            @RequestParam(required = false) LocalDate baslangic,
            @RequestParam(required = false) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokAnalizService.analiz(sirketId, id, veyaVarsayilanBaslangic(baslangic), veyaVarsayilanBitis(bitis)));
    }

    @GetMapping("/{id}/alis-ozet")
    @Operation(summary = "Alış özeti", description = "Tartılır ortalama, son, min/max alış fiyatları ve toplam alış miktarı/tutarı")
    public ResponseEntity<com.raspel.erp.dto.envanter.AlisOzetDTO> alisOzet(
            @PathVariable Long id, HttpServletRequest request,
            @RequestParam(required = false) LocalDate baslangic,
            @RequestParam(required = false) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokAnalizService.alisOzet(sirketId, id, veyaVarsayilanBaslangic(baslangic), veyaVarsayilanBitis(bitis)));
    }

    @GetMapping("/{id}/satis-ozet")
    @Operation(summary = "Satış özeti", description = "Tartılır ortalama, son, min/max satış fiyatları ve toplam satış miktarı/tutarı")
    public ResponseEntity<com.raspel.erp.dto.envanter.SatisOzetDTO> satisOzet(
            @PathVariable Long id, HttpServletRequest request,
            @RequestParam(required = false) LocalDate baslangic,
            @RequestParam(required = false) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokAnalizService.satisOzet(sirketId, id, veyaVarsayilanBaslangic(baslangic), veyaVarsayilanBitis(bitis)));
    }

    @GetMapping("/{id}/karlilik")
    @Operation(summary = "Kârlılık", description = "Brüt kâr, kâr marjı ve stok maliyeti (gerçek hareketlerden)")
    public ResponseEntity<com.raspel.erp.dto.envanter.KarlilikDTO> karlilik(
            @PathVariable Long id, HttpServletRequest request,
            @RequestParam(required = false) LocalDate baslangic,
            @RequestParam(required = false) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokAnalizService.karlilik(sirketId, id, veyaVarsayilanBaslangic(baslangic), veyaVarsayilanBitis(bitis)));
    }

    @GetMapping("/{id}/tedarikci-analiz")
    @Operation(summary = "Tedarikçi analizi", description = "Ürünün tedarikçi bazlı alış özeti")
    public ResponseEntity<List<com.raspel.erp.dto.envanter.TedarikciAnalizDTO>> tedarikciAnaliz(
            @PathVariable Long id, HttpServletRequest request,
            @RequestParam(required = false) LocalDate baslangic,
            @RequestParam(required = false) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokAnalizService.tedarikciAnaliz(sirketId, id, veyaVarsayilanBaslangic(baslangic), veyaVarsayilanBitis(bitis)));
    }

    @GetMapping("/{id}/musteri-analiz")
    @Operation(summary = "Müşteri analizi", description = "Ürünün müşteri bazlı satış özeti")
    public ResponseEntity<List<com.raspel.erp.dto.envanter.MusteriAnalizDTO>> musteriAnaliz(
            @PathVariable Long id, HttpServletRequest request,
            @RequestParam(required = false) LocalDate baslangic,
            @RequestParam(required = false) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokAnalizService.musteriAnaliz(sirketId, id, veyaVarsayilanBaslangic(baslangic), veyaVarsayilanBitis(bitis)));
    }

    @GetMapping("/{id}/islem-gecmisi")
    @Operation(summary = "İşlem geçmişi", description = "Ürünün fatura ve iadelerden oluşan tüm hareket geçmişi (tarih sıralı)")
    public ResponseEntity<List<com.raspel.erp.dto.envanter.IslemSatirDTO>> islemGecmisi(
            @PathVariable Long id, HttpServletRequest request,
            @RequestParam(required = false) LocalDate baslangic,
            @RequestParam(required = false) LocalDate bitis,
            @RequestParam(defaultValue = "false") boolean artan,
            @RequestParam(required = false) Integer limit) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokAnalizService.islemGecmisi(sirketId, id, veyaVarsayilanBaslangic(baslangic), veyaVarsayilanBitis(bitis), artan, limit));
    }

    @GetMapping("/{id}/islem-gecmisi-sayfali")
    @Operation(summary = "İşlem geçmişi (sayfalı)", description = "Toplam kayıt sayısıyla birlikte sayfalı işlem geçmişi döndürür (tarih sıralı)")
    public ResponseEntity<com.raspel.erp.dto.envanter.IslemGecmisiSayfaliDTO> islemGecmisiSayfali(
            @PathVariable Long id, HttpServletRequest request,
            @RequestParam(required = false) LocalDate baslangic,
            @RequestParam(required = false) LocalDate bitis,
            @RequestParam(defaultValue = "false") boolean artan,
            @RequestParam(required = false) Integer sayfa,
            @RequestParam(required = false) Integer boyut) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokAnalizService.islemGecmisiSayfali(sirketId, id, veyaVarsayilanBaslangic(baslangic), veyaVarsayilanBitis(bitis), artan, sayfa, boyut));
    }

    @GetMapping("/{id}/aylik-fiyat")
    @Operation(summary = "Aylık fiyat geçmişi", description = "Ay bazında tartılır ortalama alış/satış fiyatları (grafik için)")
    public ResponseEntity<List<com.raspel.erp.dto.envanter.AylikFiyatDTO>> aylikFiyat(
            @PathVariable Long id, HttpServletRequest request,
            @RequestParam(required = false) LocalDate baslangic,
            @RequestParam(required = false) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokAnalizService.aylikFiyatGecmisi(sirketId, id, veyaVarsayilanBaslangic(baslangic), veyaVarsayilanBitis(bitis)));
    }

    private LocalDate veyaVarsayilanBaslangic(LocalDate baslangic) {
        return baslangic != null ? baslangic : LocalDate.of(2000, 1, 1);
    }

    private LocalDate veyaVarsayilanBitis(LocalDate bitis) {
        return bitis != null ? bitis : LocalDate.now();
    }
}