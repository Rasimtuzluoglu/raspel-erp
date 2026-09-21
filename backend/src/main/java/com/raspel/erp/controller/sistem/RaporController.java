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
    private final com.raspel.erp.service.ticaret.FaturaGecmisService faturaGecmisService;
    private final com.raspel.erp.service.sistem.ExcelExportService excelExportService;
    private final com.raspel.erp.service.sistem.EmailService emailService;

    @PostMapping(value = "/eposta", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Rapor PDF'ini e-posta ile gönder",
            description = "Yüklenen PDF ekini belirtilen adrese gönderir (rapor paylaşımı)")
    public ResponseEntity<java.util.Map<String, String>> raporEpostaGonder(
            @RequestParam("dosya") org.springframework.web.multipart.MultipartFile dosya,
            @RequestParam String alici,
            @RequestParam(required = false) String baslik,
            @RequestParam(required = false) String dosyaAdi) {
        if (alici == null || !alici.contains("@")) {
            throw new com.raspel.erp.exception.BusinessException("Geçerli bir e-posta adresi giriniz");
        }
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
    @Operation(summary = "Yaşlandırma raporu", description = "Cari hesap yaşlandırma raporunu getirir")
    public ResponseEntity<List<RaporDTO.YaslandirmaDTO>> yaslandirma(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(raporService.yaslandirmaRaporu(sirketId));
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
    public ResponseEntity<byte[]> butceGerceklesenPdf(
            HttpServletRequest request,
            @RequestParam Integer yil,
            @RequestParam(required = false) Integer ay) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        List<com.raspel.erp.dto.sistem.ButceGerceklesenDTO> rapor = raporService.butceGerceklesenRaporu(sirketId, yil, ay);

        String[] kolonlar = {"Kategori", "Bütçe", "Gerçekleşen", "Sapma", "Kullanım %"};
        List<String[]> satirlar = rapor.stream().map(r -> new String[]{
                r.getKategori(),
                r.getButce() != null ? r.getButce().toPlainString() : "0",
                r.getGerceklesen() != null ? r.getGerceklesen().toPlainString() : "0",
                r.getSapma() != null ? r.getSapma().toPlainString() : "0",
                r.getKullanimYuzdesi() != null ? r.getKullanimYuzdesi().toPlainString() : "-"
        }).collect(java.util.stream.Collectors.toList());

        byte[] pdf = raporService.butceGerceklesenPdf(kolonlar, satirlar, yil, ay);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename("butce-gerceklesen-" + yil + (ay != null ? "-" + ay : "") + ".pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/cari-ekstre/pdf")
    @Operation(summary = "Cari ekstre PDF", description = "Cari ekstreyi PDF olarak dışa aktarır")
    public ResponseEntity<byte[]> cariEkstrePdf(
            @RequestParam Long cariHesapId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        RaporDTO.CariEkstreDTO ekstre = raporService.cariEkstreGetir(cariHesapId, baslangic, bitis);
        String[] kolonlar = {"Tarih", "Tur", "Aciklama", "Borc", "Alacak", "Bakiye"};
        List<String[]> satirlar = ekstre.getHareketler() == null ? List.of()
                : ekstre.getHareketler().stream()
                        .map(h -> new String[]{
                                h.getTarih() != null ? h.getTarih().toString() : "-",
                                h.getTur() != null ? h.getTur() : "-",
                                h.getAciklama() != null ? h.getAciklama() : "-",
                                h.getBorc() != null ? h.getBorc().toPlainString() : "0",
                                h.getAlacak() != null ? h.getAlacak().toPlainString() : "0",
                                h.getYuruyenBakiye() != null ? h.getYuruyenBakiye().toPlainString() : "0"
                        })
                        .collect(java.util.stream.Collectors.toList());
        byte[] pdf = pdfRaporService.tabloRaporu(
                "CARI EKSTRE - " + (ekstre.getCariAd() != null ? ekstre.getCariAd() : ""), kolonlar, satirlar);
        return pdfResponse("cari-ekstre-" + cariHesapId + ".pdf", pdf);
    }

    @GetMapping("/gelir-gider/pdf")
    @Operation(summary = "Gelir/Gider PDF", description = "Gelir/gider raporunu PDF olarak dışa aktarır")
    public ResponseEntity<byte[]> gelirGiderPdf(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        RaporDTO.GelirGiderOzetDTO ozet = raporService.gelirGiderOzeti(baslangic, bitis, sirketId);
        String[] kolonlar = {"Ay / Kalem", "Tutar"};
        List<String[]> satirlar = new java.util.ArrayList<>();
        if (ozet.getAylikDagilim() != null) {
            for (Map<String, Object> m : ozet.getAylikDagilim()) {
                satirlar.add(new String[]{String.valueOf(m.get("ay")), String.valueOf(m.get("net"))});
            }
        }
        satirlar.add(new String[]{"Toplam Gelir", ozet.getToplamGelir() != null ? ozet.getToplamGelir().toPlainString() : "0"});
        satirlar.add(new String[]{"Toplam Gider", ozet.getToplamGider() != null ? ozet.getToplamGider().toPlainString() : "0"});
        satirlar.add(new String[]{"Net Kar/Zarar", ozet.getNetKarZarar() != null ? ozet.getNetKarZarar().toPlainString() : "0"});
        byte[] pdf = pdfRaporService.tabloRaporu("GELIR/GIDER RAPORU (" + baslangic + " - " + bitis + ")", kolonlar, satirlar);
        return pdfResponse("gelir-gider.pdf", pdf);
    }

    @GetMapping("/cari-karlilik/pdf")
    @Operation(summary = "Cari karlilik PDF", description = "Cari karlilik raporunu PDF olarak dışa aktarır")
    public ResponseEntity<byte[]> cariKarlilikPdf(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        RaporDTO.CariKarlilikDTO rapor = raporService.cariKarlilikRaporu(baslangic, bitis, sirketId);
        String[] kolonlar = {"Cari", "Satış", "Maliyet", "Kâr", "Marj %", "Fatura"};
        List<String[]> satirlar = new java.util.ArrayList<>();
        if (rapor.getSatirlar() != null) {
            for (RaporDTO.CariKarlilikSatiriDTO s : rapor.getSatirlar()) {
                satirlar.add(new String[]{
                        s.getCariAd(),
                        s.getToplamSatis() != null ? s.getToplamSatis().toPlainString() : "0",
                        s.getToplamMaliyet() != null ? s.getToplamMaliyet().toPlainString() : "0",
                        s.getKar() != null ? s.getKar().toPlainString() : "0",
                        s.getKarMarji() != null ? s.getKarMarji().toPlainString() : "-",
                        String.valueOf(s.getFaturaSayisi())
                });
            }
        }
        satirlar.add(new String[]{
                "TOPLAM",
                rapor.getToplamSatis() != null ? rapor.getToplamSatis().toPlainString() : "0",
                rapor.getToplamMaliyet() != null ? rapor.getToplamMaliyet().toPlainString() : "0",
                rapor.getToplamKar() != null ? rapor.getToplamKar().toPlainString() : "0",
                "", ""
        });
        byte[] pdf = pdfRaporService.tabloRaporu("CARI KARLILIK RAPORU (" + baslangic + " - " + bitis + ")", kolonlar, satirlar);
        return pdfResponse("cari-karlilik.pdf", pdf);
    }

    private static final String[] FG_KOLONLAR = {
            "Tarih", "Fatura No", "Tür", "Cari", "Olay", "Kullanıcı", "Biçim", "Yazıcı", "Kopya", "Açıklama"
    };

    @GetMapping("/fatura-gecmis")
    @Operation(summary = "Fatura işlem/yazdırma geçmişi",
            description = "Oluşturma/düzenleme/durum/silme/yazdırma olaylarını filtreli olarak listeler")
    public ResponseEntity<java.util.List<com.raspel.erp.dto.ticaret.FaturaGecmisRaporDTO>> faturaGecmis(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @RequestParam(required = false) String olay,
            @RequestParam(required = false) Long kullaniciId,
            @RequestParam(required = false) String tur,
            @RequestParam(required = false) String q) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(faturaGecmisService.rapor(sirketId, baslangic, bitis, olay, kullaniciId, tur, q));
    }

    @GetMapping("/fatura-gecmis/pdf")
    @Operation(summary = "Fatura geçmişi PDF")
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