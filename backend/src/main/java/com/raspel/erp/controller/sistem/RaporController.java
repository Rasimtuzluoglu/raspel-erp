package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.RaporDTO;
import com.raspel.erp.service.sistem.RaporService;
import com.raspel.erp.service.sistem.PdfRaporService;
import com.raspel.erp.service.sistem.KarlilikService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.raspel.erp.entity.sistem.Donem;

@Tag(name = "Raporlar", description = "Raporlama API")
@RestController
@RequestMapping("/api/raporlar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class RaporController {

    private final RaporService raporService;
    private final PdfRaporService pdfRaporService;
    private final KarlilikService karlilikService;
    private final com.raspel.erp.service.sistem.Gorunum360Service gorunum360Service;
    private final com.raspel.erp.service.ticaret.FaturaGecmisService faturaGecmisService;
    private final com.raspel.erp.service.sistem.ExcelExportService excelExportService;
    private final com.raspel.erp.service.sistem.EmailService emailService;
    private final com.raspel.erp.service.sistem.EmailPolitikaService emailPolitikaService;
    private final com.raspel.erp.service.finans.DovizCevirici dovizCevirici;

    /**
     * TRY tutarı hedef para birimine çevirip plâin metne döndürür (rapor
     * dışa aktarmalarında ekrandaki değerlerle tutarlılık için). Hedef yoksa
     * ham TRY değeri döner.
     */
    private String cevirStr(BigDecimal v) {
        BigDecimal c = dovizCevirici.cevir(v);
        return c != null ? c.toPlainString() : "0";
    }

    private String cevirObj(Object v) {
        if (v == null) return "0";
        if (v instanceof BigDecimal bd) return cevirStr(bd);
        try {
            return cevirStr(new BigDecimal(v.toString()));
        } catch (Exception e) {
            return v.toString();
        }
    }

    /** PDF başlığına "(USD)" gibi para birimi sonekini ekler. */
    private String paraBirimiSonek() {
        String kod = dovizCevirici.hedef();
        return (kod != null && !"TRY".equals(kod)) ? " (" + kod + ")" : "";
    }

    /** İstek diline göre PDF metin sözlüğü (Accept-Language). */
    private com.raspel.erp.service.sistem.PdfMetin metin(HttpServletRequest request) {
        return com.raspel.erp.service.sistem.PdfMetin.of(request.getHeader("Accept-Language"));
    }

    @PostMapping(value = "/eposta", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Rapor PDF'ini e-posta ile gönder",
            description = "Yüklenen PDF ekini belirtilen adrese gönderir (rapor paylaşımı)")
    public ResponseEntity<java.util.Map<String, String>> raporEpostaGonder(
            HttpServletRequest request,
            @RequestParam("dosya") org.springframework.web.multipart.MultipartFile dosya,
            @RequestParam String alici,
            @RequestParam(required = false) String baslik,
            @RequestParam(required = false) String dosyaAdi) {
        // Serbest alıcıya gönderim (relay) engellenir; yalnızca şirketin kayıtlı e-postaları.
        Long sirketId = (Long) request.getAttribute("sirketId");
        emailPolitikaService.aliciDogrula(alici, sirketId);
        if (dosya == null || dosya.isEmpty()) {
            throw new com.raspel.erp.exception.BusinessException("Gönderilecek rapor dosyası boş");
        }
        byte[] bytes;
        try {
            bytes = dosya.getBytes();
        } catch (java.io.IOException e) {
            throw new com.raspel.erp.exception.BusinessException("Rapor dosyası okunamadı");
        }
        if (bytes.length < 4 || bytes[0] != 0x25 || bytes[1] != 0x50 || bytes[2] != 0x44 || bytes[3] != 0x46) {
            throw new com.raspel.erp.exception.BusinessException("Yalnızca PDF dosyaları e-posta ile gönderilebilir");
        }
        String raporAdi = baslik != null && !baslik.isBlank() ? baslik : "Rapor";
        String ek = dosyaAdi != null && !dosyaAdi.isBlank() ? dosyaAdi : "rapor.pdf";
        boolean gonderildi = emailService.raporPdfGonder(alici, raporAdi, bytes, ek);
        if (!gonderildi) {
            throw new com.raspel.erp.exception.BusinessException(
                    "Rapor e-postası gönderilemedi: SMTP yapılandırılmamış veya gönderim hatası");
        }
        return ResponseEntity.ok(java.util.Map.of("durum", "GONDERILDI"));
    }

    @GetMapping("/karlilik-analizi")
    @Operation(summary = "Gelişmiş kârlılık analizi",
            description = "Satış faturalarından ciro/COGS/brüt kâr; aylık trend ve grup (ürün/kategori/cari) kırılımı")
    public ResponseEntity<com.raspel.erp.dto.sistem.KarlilikAnalizDTO> karlilikAnalizi(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @RequestParam(required = false, defaultValue = "KATEGORI") String grup) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(karlilikService.karlilikAnalizi(sirketId, baslangic, bitis, grup));
    }

    @GetMapping("/karlilik-detay")
    @Operation(summary = "Kârlılık satır detayı (drill-down)",
            description = "Seçilen kategori/ürün/cari için alt kırılım ve belge (fatura kalemi) bazlı kâr dökümü")
    public ResponseEntity<com.raspel.erp.dto.sistem.KarlilikDetayDTO> karlilikDetay(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @RequestParam(required = false, defaultValue = "KATEGORI") String grup,
            @RequestParam(required = false) String deger,
            @RequestParam(required = false) Long degerId) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(karlilikService.karlilikDetay(sirketId, baslangic, bitis, grup, deger, degerId));
    }

    @GetMapping("/stok-kar-360")
    @Operation(summary = "Stok kâr 360 görünümü",
            description = "Ürün bazında ciro, maliyet, brüt kâr ve marj özeti (stok kâr durumu).")
    public ResponseEntity<com.raspel.erp.dto.sistem.Gorunum360DTO.StokKar> stokKar360(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(gorunum360Service.stokKar360(sirketId, baslangic, bitis));
    }

    @GetMapping("/calisan-performans-360")
    @Operation(summary = "Çalışan performans 360 görünümü",
            description = "Teslimat bazında çalışan (şoför) performans özeti: toplam/tamamlanan/bekleyen teslimat.")
    public ResponseEntity<com.raspel.erp.dto.sistem.Gorunum360DTO.CalisanPerformans> calisanPerformans360(
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(gorunum360Service.calisanPerformans360(sirketId));
    }

    @GetMapping("/musteri-segment")
    @Operation(summary = "Müşteri segmentasyonu",
            description = "Kural tabanlı müşteri segmentleri (VIP/DÜZENLİ/YENİ/RİSKLİ/PASİF) ve gerekçeleri.")
    public ResponseEntity<com.raspel.erp.dto.sistem.Gorunum360DTO.MusteriSegment> musteriSegment(
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(gorunum360Service.musteriSegmentasyon(sirketId));
    }

    @GetMapping("/cari-ekstre")
    @Operation(summary = "Cari ekstre getir", description = "Belirli bir cari hesabın belirtilen tarih aralığındaki ekstresini getirir")
    public ResponseEntity<RaporDTO.CariEkstreDTO> cariEkstre(
            @RequestParam Long cariHesapId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return ResponseEntity.ok(raporService.cariEkstreGetir(cariHesapId, baslangic, bitis));
    }

    @GetMapping("/gelir-gider")
    @Operation(summary = "Gelir gider raporu", description = "Belirtilen tarih aralığındaki gelir/gider özetini getirir")
    public ResponseEntity<RaporDTO.GelirGiderOzetDTO> gelirGider(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(raporService.gelirGiderOzeti(baslangic, bitis, sirketId));
    }

    @GetMapping("/kdv")
    @Operation(summary = "KDV raporu", description = "Belirtilen tarih aralığındaki KDV raporunu getirir")
    public ResponseEntity<RaporDTO.KdvRaporDTO> kdvRaporu(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(raporService.kdvRaporu(baslangic, bitis, sirketId));
    }

    @GetMapping("/yaslandirma")
    @Operation(summary = "Yaşlandırma raporu",
            description = "Cari hesap yaşlandırma raporunu kova matrisi olarak getirir. `referansTarih` verilmezse bugün esas alınır.")
    public ResponseEntity<RaporDTO.YaslandirmaRaporDTO> yaslandirma(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referansTarih) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(raporService.yaslandirmaRaporu(sirketId, referansTarih));
    }

    @GetMapping("/yaslandirma/pdf")
    @Operation(summary = "Yaşlandırma raporu PDF", description = "Vade yaşlandırma raporunu kova matrisi olarak PDF üretir")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'RAPOR_EXPORT')")
    public ResponseEntity<byte[]> yaslandirmaPdf(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referansTarih,
            @RequestParam(required = false) String doviz) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        dovizCevirici.basla(doviz);
        try {
            var m = metin(request);
            RaporDTO.YaslandirmaRaporDTO rapor = raporService.yaslandirmaRaporu(sirketId, referansTarih);
            List<String> kovalar = RaporService.YASLANDIRMA_KOVALARI;

            String[] basliklar = new String[kovalar.size() + 4];
            basliklar[0] = m.t("cari");
            for (int i = 0; i < kovalar.size(); i++) {
                basliklar[i + 1] = yaslandirmaKovaEtiketi(kovalar.get(i));
            }
            basliklar[kovalar.size() + 1] = m.t("toplam");
            basliklar[kovalar.size() + 2] = m.t("gecikmis");
            basliklar[kovalar.size() + 3] = m.t("maksGun");

            List<String[]> satirlar = new ArrayList<>();
            for (RaporDTO.YaslandirmaDTO s : rapor.getSatirlar()) {
                String[] satir = new String[basliklar.length];
                satir[0] = s.getCariAd();
                for (int i = 0; i < kovalar.size(); i++) {
                    satir[i + 1] = formatTutar(s.getKovalar().get(kovalar.get(i)));
                }
                satir[kovalar.size() + 1] = formatTutar(s.getToplam());
                satir[kovalar.size() + 2] = formatTutar(s.getGecikmisTutar());
                satir[kovalar.size() + 3] = String.valueOf(s.getEnFazlaGecikmeGun());
                satirlar.add(satir);
            }

            byte[] pdf = pdfRaporService.tabloRaporu(
                    m.t("vadeYaslandirma") + " - " + rapor.getReferansTarih() + paraBirimiSonek(), basliklar, satirlar);
            return pdfResponse("yaslandirma-" + rapor.getReferansTarih() + ".pdf", pdf);
        } finally {
            dovizCevirici.temizle();
        }
    }

    /** Kova anahtarını PDF başlığı için okunur metne çevirir. */
    private String yaslandirmaKovaEtiketi(String kova) {
        return switch (kova) {
            case "VADEDI_GELMEMIS" -> "Vadesi Gelmemis";
            case "GUN_0_30" -> "0-30 Gun";
            case "GUN_31_60" -> "31-60 Gun";
            case "GUN_61_90" -> "61-90 Gun";
            default -> "90+ Gun";
        };
    }

    private String formatTutar(BigDecimal v) {
        // Sunucunun VARSAYILAN locale'ine gore formatlamak (ör. en-US) PDF/rapor
        // basligi ve tutarlarla (tr-TR) çelişiyordu; sabit tr-TR kullanılır.
        // Tutar, hedef para birimi ayarlıysa çevrilir.
        BigDecimal c = dovizCevirici.cevir(v);
        return c == null ? "-" : String.format(java.util.Locale.forLanguageTag("tr-TR"), "%,.2f", c);
    }

    @GetMapping("/kdv-beyanname")
    @Operation(summary = "KDV beyanname hazırlığı", description = "YYYY-MM dönemi için KDV beyannameye hazırlık listesi üretir (matrah + KDV oran bazlı)")
    public ResponseEntity<RaporDTO.KdvBeyannameDTO> kdvBeyanname(HttpServletRequest request, @RequestParam String donem) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(raporService.kdvBeyannameGetir(donem, sirketId));
    }

    @GetMapping("/ba-bs")
    @Operation(summary = "BA/BS bildirimi", description = "YYYY-MM dönemi için BA (alış) veya BS (satış) bildirim formu listesi üretir")
    public ResponseEntity<RaporDTO.BaBsDTO> baBs(
            HttpServletRequest request,
            @RequestParam String donem,
            @RequestParam(defaultValue = "BS") String tur,
            @RequestParam(required = false) BigDecimal esik) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(raporService.baBsGetir(donem, tur, esik, sirketId));
    }

    @GetMapping("/cari-karlilik")
    @Operation(summary = "Cari karlılık raporu", description = "Belirtilen tarih aralığında her cari hesabın satış, maliyet ve kârını getirir")
    public ResponseEntity<RaporDTO.CariKarlilikDTO> cariKarlilik(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(raporService.cariKarlilikRaporu(baslangic, bitis, sirketId));
    }

    @GetMapping("/tedarikci-urunler")
    @Operation(summary = "Tedarikçi bazlı ürün raporu", description = "Hangi tedarikçiden hangi ürünlerin geldiğini (toplam miktar, son fiyat, son tarih) getirir")
    public ResponseEntity<List<com.raspel.erp.dto.sistem.TedarikciUrunDTO>> tedarikciUrunler(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(raporService.tedarikciUrunRaporu(sirketId));
    }

    @GetMapping("/urun-karlilik")
    @Operation(summary = "Ürün kârlılık raporu", description = "Her ürünün alış maliyeti, satış fiyatı ve kâr marjını getirir")
    public ResponseEntity<List<com.raspel.erp.dto.sistem.UrunKarlilikDTO>> urunKarlilik(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(raporService.urunKarlilikRaporu(sirketId));
    }

    @GetMapping("/stok-degerleme")
    @Operation(summary = "Stok değerleme raporu",
            description = "Stokların ağırlıklı ortalama ve FIFO (katman bazlı) değerini karşılaştırmalı getirir")
    public ResponseEntity<com.raspel.erp.dto.sistem.StokAnalizDTO.Degerleme> stokDegerleme(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(raporService.stokDegerleme(sirketId));
    }

    @GetMapping("/siparis-onerisi")
    @Operation(summary = "Sipariş önerisi",
            description = "Minimum seviyenin altına düşen stoklar için hedefe tamamlama önerisi (min x2) üretir")
    public ResponseEntity<List<com.raspel.erp.dto.sistem.StokAnalizDTO.OneriSatiri>> siparisOnerisi(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(raporService.siparisOnerisi(sirketId));
    }

    @GetMapping("/temsilci-performans")
    @Operation(summary = "Temsilci performans raporu",
            description = "Tarih aralığında cari kartındaki satış temsilcisine göre satış toplamlarını getirir")
    public ResponseEntity<com.raspel.erp.dto.sistem.StokAnalizDTO.TemsilciPerformans> temsilciPerformans(
            HttpServletRequest request,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate baslangic,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        java.time.LocalDate bas = baslangic != null ? baslangic : java.time.LocalDate.now().withDayOfYear(1);
        java.time.LocalDate bit = bitis != null ? bitis : java.time.LocalDate.now();
        return ResponseEntity.ok(raporService.temsilciPerformans(sirketId, bas, bit));
    }

    @GetMapping("/nakit-akisi-projeksiyonu")
    @Operation(summary = "Nakit akışı projeksiyonu", description = "30/60/90 günlük tahmini nakit akışı ve kasa projeksiyonunu getirir")
    public ResponseEntity<com.raspel.erp.dto.sistem.NakitAkisiProjeksiyonDTO> nakitAkisiProjeksiyonu(
            HttpServletRequest request,
            @RequestParam(defaultValue = "30") int gun) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(raporService.nakitAkisiProjeksiyonu(gun, sirketId));
    }

    @GetMapping("/butce-gerceklesen")
    @Operation(summary = "Bütçe vs Gerçekleşen raporu", description = "Kategori bazlı planlanan bütçe ile gerçekleşen masrafı karşılaştırır")
    public ResponseEntity<List<com.raspel.erp.dto.sistem.ButceGerceklesenDTO>> butceGerceklesen(
            HttpServletRequest request,
            @RequestParam Integer yil,
            @RequestParam(required = false) Integer ay) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(raporService.butceGerceklesenRaporu(sirketId, yil, ay));
    }

    @GetMapping("/pivot")
    @Operation(summary = "Dinamik pivot tablo", description = "Satır/sütun/değer boyutlarına göre fatura kalemlerini çaprazlar ve özetler")
    public ResponseEntity<com.raspel.erp.dto.sistem.PivotDTO> pivot(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "cari") String satir,
            @RequestParam(required = false, defaultValue = "ay") String sutun,
            @RequestParam(required = false, defaultValue = "tutar") String deger,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(raporService.pivot(sirketId, satir, sutun, deger, baslangic, bitis));
    }

    @GetMapping("/butce-gerceklesen/pdf")
    @Operation(summary = "Bütçe vs Gerçekleşen PDF", description = "Bütçe vs gerçekleşen raporunu PDF olarak dışa aktarır")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'RAPOR_EXPORT')")
    public ResponseEntity<byte[]> butceGerceklesenPdf(
            HttpServletRequest request,
            @RequestParam Integer yil,
            @RequestParam(required = false) Integer ay,
            @RequestParam(required = false) String doviz) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        dovizCevirici.basla(doviz);
        try {
            var m = metin(request);
            List<com.raspel.erp.dto.sistem.ButceGerceklesenDTO> rapor = raporService.butceGerceklesenRaporu(sirketId, yil, ay);

            String[] kolonlar = {m.t("kategori"), m.t("butce"), m.t("gerceklesen"), m.t("sapma"), m.t("kullanimYuzdesi")};
            List<String[]> satirlar = rapor.stream().map(r -> new String[]{
                    r.getKategori(),
                    cevirStr(r.getButce()),
                    cevirStr(r.getGerceklesen()),
                    cevirStr(r.getSapma()),
                    r.getKullanimYuzdesi() != null ? r.getKullanimYuzdesi().toPlainString() : "-"
            }).collect(java.util.stream.Collectors.toList());

            byte[] pdf = raporService.butceGerceklesenPdf(kolonlar, satirlar, yil, ay);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                    .filename("butce-gerceklesen-" + yil + (ay != null ? "-" + ay : "") + ".pdf").build());
            return ResponseEntity.ok().headers(headers).body(pdf);
        } finally {
            dovizCevirici.temizle();
        }
    }

    @GetMapping("/cari-ekstre/pdf")
    @Operation(summary = "Cari ekstre PDF", description = "Cari ekstreyi PDF olarak dışa aktarır")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'CARI_EXPORT')")
    public ResponseEntity<byte[]> cariEkstrePdf(
            HttpServletRequest request,
            @RequestParam Long cariHesapId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @RequestParam(required = false) String doviz) {
        dovizCevirici.basla(doviz);
        try {
            var m = metin(request);
            RaporDTO.CariEkstreDTO ekstre = raporService.cariEkstreGetir(cariHesapId, baslangic, bitis);
            String[] kolonlar = {m.t("tarih"), m.t("tur"), m.t("aciklama"), m.t("borc"), m.t("alacak"), m.t("bakiye")};
            List<String[]> satirlar = ekstre.getHareketler() == null ? List.of()
                    : ekstre.getHareketler().stream()
                            .map(h -> new String[]{
                                    h.getTarih() != null ? h.getTarih().toString() : "-",
                                    h.getTur() != null ? h.getTur() : "-",
                                    h.getAciklama() != null ? h.getAciklama() : "-",
                                    cevirStr(h.getBorc()),
                                    cevirStr(h.getAlacak()),
                                    cevirStr(h.getYuruyenBakiye())
                            })
                            .collect(java.util.stream.Collectors.toList());
            byte[] pdf = pdfRaporService.tabloRaporu(
                    m.t("cariEkstre") + " - " + (ekstre.getCariAd() != null ? ekstre.getCariAd() : "") + paraBirimiSonek(),
                    kolonlar, satirlar);
            return pdfResponse("cari-ekstre-" + cariHesapId + ".pdf", pdf);
        } finally {
            dovizCevirici.temizle();
        }
    }

    @GetMapping("/gelir-gider/pdf")
    @Operation(summary = "Gelir/Gider PDF", description = "Gelir/gider raporunu PDF olarak dışa aktarır")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'RAPOR_EXPORT')")
    public ResponseEntity<byte[]> gelirGiderPdf(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @RequestParam(required = false) String doviz) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        dovizCevirici.basla(doviz);
        try {
            var m = metin(request);
            RaporDTO.GelirGiderOzetDTO ozet = raporService.gelirGiderOzeti(baslangic, bitis, sirketId);
            String[] kolonlar = {m.t("ayKalem"), m.t("tutar")};
            List<String[]> satirlar = new java.util.ArrayList<>();
            if (ozet.getAylikDagilim() != null) {
                for (Map<String, Object> mm : ozet.getAylikDagilim()) {
                    satirlar.add(new String[]{String.valueOf(mm.get("ay")), cevirObj(mm.get("net"))});
                }
            }
            satirlar.add(new String[]{m.t("toplamGelir"), cevirStr(ozet.getToplamGelir())});
            satirlar.add(new String[]{m.t("toplamGider"), cevirStr(ozet.getToplamGider())});
            satirlar.add(new String[]{m.t("netKarZarar"), cevirStr(ozet.getNetKarZarar())});
            byte[] pdf = pdfRaporService.tabloRaporu(
                    m.t("gelirGiderRaporu") + " (" + baslangic + " - " + bitis + ")" + paraBirimiSonek(), kolonlar, satirlar);
            return pdfResponse("gelir-gider.pdf", pdf);
        } finally {
            dovizCevirici.temizle();
        }
    }

    @GetMapping("/cari-karlilik/pdf")
    @Operation(summary = "Cari karlilik PDF", description = "Cari karlilik raporunu PDF olarak dışa aktarır")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'RAPOR_EXPORT')")
    public ResponseEntity<byte[]> cariKarlilikPdf(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @RequestParam(required = false) String doviz) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        dovizCevirici.basla(doviz);
        try {
            var m = metin(request);
            RaporDTO.CariKarlilikDTO rapor = raporService.cariKarlilikRaporu(baslangic, bitis, sirketId);
            String[] kolonlar = {m.t("cari"), m.t("satis"), m.t("maliyet"), m.t("kar"), m.t("marjYuzdesi"), m.t("fatura")};
            List<String[]> satirlar = new java.util.ArrayList<>();
            if (rapor.getSatirlar() != null) {
                for (RaporDTO.CariKarlilikSatiriDTO s : rapor.getSatirlar()) {
                    satirlar.add(new String[]{
                            s.getCariAd(),
                            cevirStr(s.getToplamSatis()),
                            cevirStr(s.getToplamMaliyet()),
                            cevirStr(s.getKar()),
                            s.getKarMarji() != null ? s.getKarMarji().toPlainString() : "-",
                            String.valueOf(s.getFaturaSayisi())
                    });
                }
            }
            satirlar.add(new String[]{
                    m.t("toplam"),
                    cevirStr(rapor.getToplamSatis()),
                    cevirStr(rapor.getToplamMaliyet()),
                    cevirStr(rapor.getToplamKar()),
                    "", ""
            });
            byte[] pdf = pdfRaporService.tabloRaporu(
                    m.t("cariKarlilikRaporu") + " (" + baslangic + " - " + bitis + ")" + paraBirimiSonek(), kolonlar, satirlar);
            return pdfResponse("cari-karlilik.pdf", pdf);
        } finally {
            dovizCevirici.temizle();
        }
    }

    private static final String[] FG_KOLONLAR = {
            "Tarih", "Fatura No", "Tür", "Cari", "Olay", "Kullanıcı", "Biçim", "Yazıcı", "Kopya", "Açıklama"
    };

    @GetMapping("/fatura-gecmis")
    @Operation(summary = "Fatura işlem/yazdırma geçmişi (sayfalı)",
            description = "Oluşturma/düzenleme/durum/silme/yazdırma olaylarını filtreli ve sayfalı olarak listeler")
    public ResponseEntity<org.springframework.data.domain.Page<com.raspel.erp.dto.ticaret.FaturaGecmisRaporDTO>> faturaGecmis(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @RequestParam(required = false) String olay,
            @RequestParam(required = false) Long kullaniciId,
            @RequestParam(required = false) String tur,
            @RequestParam(required = false) String q,
            @org.springframework.data.web.PageableDefault(size = 50) org.springframework.data.domain.Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(faturaGecmisService.raporSayfali(
                sirketId, baslangic, bitis, olay, kullaniciId, tur, q, pageable));
    }

    @GetMapping("/fatura-gecmis/pdf")
    @Operation(summary = "Fatura geçmişi PDF")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'FATURA_EXPORT')")
    public ResponseEntity<byte[]> faturaGecmisPdf(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @RequestParam(required = false) String olay,
            @RequestParam(required = false) Long kullaniciId,
            @RequestParam(required = false) String tur,
            @RequestParam(required = false) String q) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var kayitlar = faturaGecmisService.rapor(sirketId, baslangic, bitis, olay, kullaniciId, tur, q);
        List<String[]> satirlar = new java.util.ArrayList<>();
        for (var k : kayitlar) satirlar.add(fgSatir(k));
        byte[] pdf = pdfRaporService.tabloRaporu("FATURA ISLEM/YAZDIRMA GECMISI", FG_KOLONLAR, satirlar);
        return pdfResponse("fatura-gecmis.pdf", pdf);
    }

    @GetMapping("/fatura-gecmis/excel")
    @Operation(summary = "Fatura geçmişi Excel")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'FATURA_EXPORT')")
    public ResponseEntity<byte[]> faturaGecmisExcel(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @RequestParam(required = false) String olay,
            @RequestParam(required = false) Long kullaniciId,
            @RequestParam(required = false) String tur,
            @RequestParam(required = false) String q) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var kayitlar = faturaGecmisService.rapor(sirketId, baslangic, bitis, olay, kullaniciId, tur, q);
        List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        for (var k : kayitlar) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            String[] s = fgSatir(k);
            for (int i = 0; i < FG_KOLONLAR.length; i++) m.put(FG_KOLONLAR[i], s[i]);
            rows.add(m);
        }
        byte[] xlsx = excelExportService.export("Fatura Geçmişi", FG_KOLONLAR, rows);
        return xlsxResponse("fatura-gecmis.xlsx", xlsx);
    }

    private String[] fgSatir(com.raspel.erp.dto.ticaret.FaturaGecmisRaporDTO k) {
        return new String[]{
                k.getTarih() != null ? k.getTarih().toString() : "",
                k.getFaturaNumarasi() != null ? k.getFaturaNumarasi() : "",
                k.getFaturaTur() != null ? k.getFaturaTur() : "",
                k.getCariHesapAd() != null ? k.getCariHesapAd() : "",
                k.getOlay() != null ? k.getOlay() : "",
                k.getKullaniciAdi() != null ? k.getKullaniciAdi() : "",
                k.getYazdirmaFormat() != null ? k.getYazdirmaFormat() : "",
                k.getYaziciAdi() != null ? k.getYaziciAdi() : "",
                k.getKopyaNo() != null ? String.valueOf(k.getKopyaNo()) : "",
                k.getAciklama() != null ? k.getAciklama() : ""
        };
    }

    private ResponseEntity<byte[]> xlsxResponse(String dosyaAdi, byte[] xlsx) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename(dosyaAdi).build());
        return ResponseEntity.ok().headers(headers).body(xlsx);
    }

    private ResponseEntity<byte[]> pdfResponse(String dosyaAdi, byte[] pdf) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename(dosyaAdi).build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}