package com.raspel.erp.controller.sistem;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.data.domain.Pageable;
import com.raspel.erp.service.sistem.AuditLogService;
import com.raspel.erp.service.finans.BankaService;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.service.sistem.ExcelExportService;
import com.raspel.erp.service.muhasebe.MuhasebeService;
import com.raspel.erp.dto.muhasebe.BilancoDTO;
import com.raspel.erp.dto.muhasebe.KarZararDTO;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.service.ticaret.FaturaService;
import com.raspel.erp.service.finans.HareketService;
import com.raspel.erp.service.finans.KasaService;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.service.ik.PersonelService;
import com.raspel.erp.service.envanter.StokService;

@Tag(name = "Excel Dışa Aktarım", description = "Excel dosyası olarak dışa aktarma API")
@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class ExcelExportController {

    private static final int MAX_EXPORT_ROWS = 10000;

    private final ExcelExportService excelService;
    private final CariHesapService cariHesapService;
    private final FaturaService faturaService;
    private final HareketService hareketService;
    private final StokService stokService;
    private final PersonelService personelService;
    private final BankaService bankaService;
    private final KasaService kasaService;
    private final AuditLogService auditLogService;
    private final MuhasebeService muhasebeService;

    @GetMapping("/cari-hesaplar")
    @Operation(summary = "Cari hesapları Excel dışa aktar", description = "Cari hesapları Excel (.xlsx) dosyası olarak dışa aktarır")
    public ResponseEntity<byte[]> cariHesaplar(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = cariHesapService.tumCariHesaplariGetir(sirketId, Pageable.unpaged()).getContent();
        var rows = list.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", c.getId()); m.put("Ad", c.getAd()); m.put("Vergi No", c.getVergiNumarasi());
            m.put("Telefon", c.getTelefon()); m.put("E-posta", c.getEmail()); m.put("Bakiye", c.getBakiye());
            return m;
        }).toList();
        return excel("CariHesaplar", new String[]{"ID", "Ad", "Vergi No", "Telefon", "E-posta", "Bakiye"}, rows);
    }

    @GetMapping("/faturalar")
    @Operation(summary = "Faturaları Excel dışa aktar", description = "Faturaları Excel (.xlsx) dosyası olarak dışa aktarır")
    public ResponseEntity<byte[]> faturalar(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = faturaService.tumFaturalariGetir(sirketId, Pageable.unpaged()).getContent();
        var rows = list.stream().map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", f.getId()); m.put("Fatura No", f.getFaturaNumarasi()); m.put("Tarih", f.getTarih());
            m.put("Müşteri", f.getCariHesapAd()); m.put("Tutar", f.getGenelToplam()); m.put("Durum", f.getDurum());
            return m;
        }).toList();
        return excel("Faturalar", new String[]{"ID", "Fatura No", "Tarih", "Müşteri", "Tutar", "Durum"}, rows);
    }

    @GetMapping("/hareketler")
    @Operation(summary = "Hareketleri Excel dışa aktar", description = "Hareketleri Excel (.xlsx) dosyası olarak dışa aktarır")
    public ResponseEntity<byte[]> hareketler(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = hareketService.tumHareketleriGetir(sirketId, Pageable.unpaged()).getContent();
        var rows = list.stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", h.getId()); m.put("Cari", h.getCariHesapAd()); m.put("Tür", h.getTur());
            m.put("Tutar", h.getTutar()); m.put("Tarih", h.getHareketTarihi()); m.put("Açıklama", h.getAciklama());
            return m;
        }).toList();
        return excel("Hareketler", new String[]{"ID", "Cari", "Tür", "Tutar", "Tarih", "Açıklama"}, rows);
    }

    @GetMapping("/stoklar")
    @Operation(summary = "Stokları Excel dışa aktar", description = "Stokları Excel (.xlsx) dosyası olarak dışa aktarır")
    public ResponseEntity<byte[]> stoklar(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = stokService.tumunuGetir(sirketId, Pageable.unpaged()).getContent();
        var rows = list.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", s.getId()); m.put("Ad", s.getAd()); m.put("Barkod", s.getBarkod());
            m.put("Kategori", s.getKategori()); m.put("Birim", s.getBirim()); m.put("Miktar", s.getMiktar());
            m.put("Alış Fiyat", s.getFiyat()); m.put("Satış Fiyat", s.getSatisFiyati());
            return m;
        }).toList();
        return excel("Stoklar", new String[]{"ID", "Ad", "Barkod", "Kategori", "Birim", "Miktar", "Alış Fiyat", "Satış Fiyat"}, rows);
    }

    @GetMapping("/personel")
    @Operation(summary = "Personeli Excel dışa aktar", description = "Personel kayıtlarını Excel (.xlsx) dosyası olarak dışa aktarır")
    public ResponseEntity<byte[]> personel(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = personelService.tumunuGetir(sirketId, Pageable.unpaged()).getContent();
        var rows = list.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", p.getId()); m.put("Ad", p.getAd()); m.put("Soyad", p.getSoyad());
            m.put("TC", p.getTcKimlik()); m.put("Telefon", p.getTelefon()); m.put("Departman", p.getDepartman());
            m.put("Pozisyon", p.getPozisyon()); m.put("Maaş", p.getMaas());
            return m;
        }).toList();
        return excel("Personel", new String[]{"ID", "Ad", "Soyad", "TC", "Telefon", "Departman", "Pozisyon", "Maaş"}, rows);
    }

    @GetMapping("/bankalar")
    @Operation(summary = "Bankaları Excel dışa aktar", description = "Bankaları Excel (.xlsx) dosyası olarak dışa aktarır")
    public ResponseEntity<byte[]> bankalar(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = bankaService.tumBankalariGetir(sirketId, Pageable.unpaged()).getContent();
        var rows = list.stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", b.getId()); m.put("Ad", b.getAd());
            m.put("Hesap No", b.getHesapNo()); m.put("IBAN", b.getIban()); m.put("Bakiye", b.getBakiye());
            return m;
        }).toList();
        return excel("Bankalar", new String[]{"ID", "Ad", "Hesap No", "IBAN", "Bakiye"}, rows);
    }

    @GetMapping("/kasalar")
    @Operation(summary = "Kasaları Excel dışa aktar", description = "Kasaları Excel (.xlsx) dosyası olarak dışa aktarır")
    public ResponseEntity<byte[]> kasalar(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = kasaService.tumKasalarGetir(sirketId, Pageable.unpaged()).getContent();
        var rows = list.stream().map(k -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", k.getId()); m.put("Ad", k.getAd()); m.put("Bakiye", k.getBakiye());
            return m;
        }).toList();
        return excel("Kasalar", new String[]{"ID", "Ad", "Bakiye"}, rows);
    }

    @GetMapping("/denetim-log")
    @Operation(summary = "Denetim loglarını Excel dışa aktar", description = "Denetim log kayıtlarını Excel (.xlsx) dosyası olarak dışa aktarır")
    public ResponseEntity<byte[]> denetimLog(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam(required = false) Long kullaniciId,
            @RequestParam(required = false) String islem,
            @RequestParam(required = false) String entityAdi,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate baslangicTarih,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bitisTarih) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var list = auditLogService.filtreliGetir(sirketId, kullaniciId, islem, entityAdi, baslangicTarih, bitisTarih, Pageable.unpaged()).getContent();
        var rows = list.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", l.getId()); m.put("Tarih", l.getTarih()); m.put("Kullanıcı ID", l.getKullaniciId());
            m.put("İşlem", l.getIslem()); m.put("Entity", l.getEntityAdi()); m.put("Entity ID", l.getEntityId());
            m.put("Açıklama", l.getAciklama()); m.put("IP", l.getIpAdresi());
            return m;
        }).toList();
        return excel("DenetimLog", new String[]{"ID", "Tarih", "Kullanıcı ID", "İşlem", "Entity", "Entity ID", "Açıklama", "IP"}, rows);
    }

    @GetMapping("/bilanco")
    @Operation(summary = "Bilançoyu Excel dışa aktar", description = "Bilançoyu (aktif/pasif) Excel (.xlsx) dosyası olarak dışa aktarır")
    public ResponseEntity<byte[]> bilanco(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        BilancoDTO b = muhasebeService.bilancoGetir(sirketId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (BilancoDTO.KalemDTO k : b.getAktifler()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("Taraf", "AKTIF"); m.put("Kod", k.getKod()); m.put("Hesap", k.getAd()); m.put("Tutar", k.getTutar());
            rows.add(m);
        }
        for (BilancoDTO.KalemDTO k : b.getPasifler()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("Taraf", "PASIF"); m.put("Kod", k.getKod()); m.put("Hesap", k.getAd()); m.put("Tutar", k.getTutar());
            rows.add(m);
        }
        Map<String, Object> aktifToplam = new LinkedHashMap<>();
        aktifToplam.put("Taraf", "TOPLAM"); aktifToplam.put("Kod", ""); aktifToplam.put("Hesap", "Aktif Toplam"); aktifToplam.put("Tutar", b.getAktifToplam());
        rows.add(aktifToplam);
        Map<String, Object> pasifToplam = new LinkedHashMap<>();
        pasifToplam.put("Taraf", "TOPLAM"); pasifToplam.put("Kod", ""); pasifToplam.put("Hesap", "Pasif Toplam"); pasifToplam.put("Tutar", b.getPasifToplam());
        rows.add(pasifToplam);
        return excel("Bilanco", new String[]{"Taraf", "Kod", "Hesap", "Tutar"}, rows);
    }

    @GetMapping("/kar-zarar")
    @Operation(summary = "Kâr/Zararı Excel dışa aktar", description = "Kâr/Zarar (gelir tablosu) özetini Excel (.xlsx) dosyası olarak dışa aktarır")
    public ResponseEntity<byte[]> karZarar(
            HttpServletRequest req,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate baslangic,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bitis) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        KarZararDTO kz = muhasebeService.karZararGetir(sirketId, baslangic, bitis);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (KarZararDTO.KalemDTO k : kz.getGelirler()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("Tur", "GELIR"); m.put("Kod", k.getKod()); m.put("Hesap", k.getAd()); m.put("Tutar", k.getTutar());
            rows.add(m);
        }
        for (KarZararDTO.KalemDTO k : kz.getGiderler()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("Tur", "GIDER"); m.put("Kod", k.getKod()); m.put("Hesap", k.getAd()); m.put("Tutar", k.getTutar());
            rows.add(m);
        }
        Map<String, Object> net = new LinkedHashMap<>();
        net.put("Tur", "NET"); net.put("Kod", ""); net.put("Hesap", "Net Kâr/Zarar"); net.put("Tutar", kz.getNetKar());
        rows.add(net);
        return excel("KarZarar", new String[]{"Tur", "Kod", "Hesap", "Tutar"}, rows);
    }

    private ResponseEntity<byte[]> excel(String name, String[] cols, List<Map<String, Object>> rows) {
        byte[] data = excelService.export(name, cols, rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "-" + java.time.LocalDate.now() + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}