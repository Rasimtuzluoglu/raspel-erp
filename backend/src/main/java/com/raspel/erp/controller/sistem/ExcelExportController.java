package com.raspel.erp.controller.sistem;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class ExcelExportController {

    private static final int MAX_EXPORT_ROWS = 10000;
    private static final Pageable MAX_EXPORT_PAGE = PageRequest.of(0, MAX_EXPORT_ROWS);

    private final ExcelExportService excelService;
    private final CariHesapService cariHesapService;
    private final FaturaService faturaService;
    private final HareketService hareketService;
    private final StokService stokService;
    private final PersonelService personelService;
    private final com.raspel.erp.service.ik.PersonelIzinService personelIzinService;
    private final com.raspel.erp.service.ik.PersonelPuantajService personelPuantajService;
    private final com.raspel.erp.service.ik.VardiyaService vardiyaService;
    private final BankaService bankaService;
    private final KasaService kasaService;
    private final AuditLogService auditLogService;
    private final MuhasebeService muhasebeService;
    private final com.raspel.erp.service.sistem.RaporService raporService;
    private final com.raspel.erp.service.ticaret.SiparisTakipService siparisTakipService;
    private final com.raspel.erp.service.ticaret.TeslimatService teslimatService;
    private final com.raspel.erp.service.envanter.UretimService uretimService;
    private final com.raspel.erp.service.sistem.Gorunum360Service gorunum360Service;
    private final com.raspel.erp.service.sistem.DonemService donemService;
    private final com.raspel.erp.service.envanter.StokSayimService stokSayimService;
    private final com.raspel.erp.service.envanter.StokSeriService stokSeriService;
    private final com.raspel.erp.service.sistem.AnomaliTespitEngine anomaliTespitEngine;
    private final com.raspel.erp.service.sistem.IpWhitelistService ipWhitelistService;
    private final com.raspel.erp.service.sube.SubeService subeService;

    @GetMapping("/kdv-beyanname")
    @Operation(summary = "KDV beyannamesini Excel dışa aktar",
            description = "YYYY-MM dönemi KDV beyanname hazırlığını (matrah + KDV oran bazlı) Excel (.xlsx) olarak dışa aktarır")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'RAPOR_EXPORT')")
    public ResponseEntity<byte[]> kdvBeyanname(HttpServletRequest req, @RequestParam String donem) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var kdv = raporService.kdvBeyannameGetir(donem, sirketId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (var s : kdv.getSatislar()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("Tablo", "HESAPLANAN"); m.put("Oran", s.getKdvOrani()); m.put("Matrah", s.getMatrah()); m.put("KDV", s.getKdv());
            rows.add(m);
        }
        for (var s : kdv.getAlislar()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("Tablo", "INDIRILECEK"); m.put("Oran", s.getKdvOrani()); m.put("Matrah", s.getMatrah()); m.put("KDV", s.getKdv());
            rows.add(m);
        }
        Map<String, Object> ozet = new LinkedHashMap<>();
        ozet.put("Tablo", "OZET"); ozet.put("Oran", "");
        ozet.put("Matrah", "Hesaplanan: " + kdv.getToplamHesaplananKdv()
                + " | İndirilecek: " + kdv.getToplamIndirilecekKdv()
                + " | Ödenecek: " + kdv.getOdenecekKdv()
                + " | Devreden: " + kdv.getDevredenKdv());
        ozet.put("KDV", "");
        rows.add(ozet);
        return excel("KDVBeyanname-" + donem, new String[]{"Tablo", "Oran", "Matrah", "KDV"}, rows);
    }

    @GetMapping("/ba-bs")
    @Operation(summary = "BA/BS formunu Excel dışa aktar",
            description = "Belirtilen dönem için BA (alış) veya BS (satış) formu kayıtlarını Excel (.xlsx) olarak dışa aktarır")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'RAPOR_EXPORT')")
    public ResponseEntity<byte[]> baBs(
            HttpServletRequest req,
            @RequestParam String donem,
            @RequestParam(defaultValue = "BS") String tur,
            @RequestParam(required = false) java.math.BigDecimal esik) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var babs = raporService.baBsGetir(donem, tur, esik, sirketId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (var k : babs.getKayitlar()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("Fatura No", k.getFaturaNo()); m.put("Tarih", k.getTarih());
            m.put("Cari", k.getCariAd()); m.put("VKN", k.getCariVkn());
            m.put("Matrah", k.getMatrah()); m.put("KDV", k.getKdv()); m.put("Tutar", k.getTutar());
            rows.add(m);
        }
        return excel("BA-BS-" + babs.getTur() + "-" + donem,
                new String[]{"Fatura No", "Tarih", "Cari", "VKN", "Matrah", "KDV", "Tutar"}, rows);
    }

    @GetMapping("/cari-hesaplar")
    @Operation(summary = "Cari hesapları Excel dışa aktar", description = "Cari hesapları Excel (.xlsx) dosyası olarak dışa aktarır")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'CARI_EXPORT')")
    public ResponseEntity<byte[]> cariHesaplar(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = cariHesapService.tumCariHesaplariGetir(sirketId, MAX_EXPORT_PAGE).getContent();
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
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'FATURA_EXPORT')")
    public ResponseEntity<byte[]> faturalar(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = faturaService.tumFaturalariGetir(sirketId, MAX_EXPORT_PAGE).getContent();
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
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'FINANS_EXPORT')")
    public ResponseEntity<byte[]> hareketler(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = hareketService.tumHareketleriGetir(sirketId, MAX_EXPORT_PAGE).getContent();
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
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'STOK_EXPORT')")
    public ResponseEntity<byte[]> stoklar(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = stokService.tumunuGetir(sirketId, MAX_EXPORT_PAGE).getContent();
        var rows = list.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", s.getId()); m.put("Ad", s.getAd()); m.put("Stok Kodu", s.getStokKodu()); m.put("Barkod", s.getBarkod());
            // Sınıflandırma alanları Excel'e de yazılır: `/api/import/stok` bu
            // kolonları okuyor, böylece dışa aktar → içe aktar turunda veri kaybı
            // olmaz. Önceden yalnız Kategori export edildiği için üretim tipi,
            // marka ve stok kodu geri yüklenemiyordu.
            m.put("Kategori", s.getKategori());
            m.put("StokGrubu", s.getStokGrubu());
            m.put("Marka", s.getMarka());
            m.put("Birim", s.getBirim()); m.put("Miktar", s.getMiktar());
            m.put("Alış Fiyat", s.getFiyat()); m.put("Satış Fiyat", s.getSatisFiyati());
            return m;
        }).toList();
        return excel("Stoklar", new String[]{"ID", "Ad", "Stok Kodu", "Barkod", "Kategori", "StokGrubu",
                "Marka", "Birim", "Miktar", "Alış Fiyat", "Satış Fiyat"}, rows);
    }

    @GetMapping("/personel")
    @Operation(summary = "Personeli Excel dışa aktar", description = "Personel kayıtlarını (isteğe bağlı arama/departman/rol/durum filtresiyle) Excel (.xlsx) dosyası olarak dışa aktarır")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IK_EXPORT')")
    public ResponseEntity<byte[]> personel(
            HttpServletRequest req,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String departman,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String durum) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = personelService.filtreli(sirketId, q, departman, rol, durum, MAX_EXPORT_PAGE).getContent();
        boolean maasGoster = maasGorebilir();
        var rows = list.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", p.getId()); m.put("Ad", p.getAd()); m.put("Soyad", p.getSoyad());
            m.put("TC", p.getTcKimlik()); m.put("Telefon", p.getTelefon()); m.put("Departman", p.getDepartman());
            m.put("Pozisyon", p.getPozisyon());
            if (maasGoster) m.put("Maaş", p.getMaas());
            return m;
        }).toList();
        String[] kolonlar = maasGoster
                ? new String[]{"ID", "Ad", "Soyad", "TC", "Telefon", "Departman", "Pozisyon", "Maaş"}
                : new String[]{"ID", "Ad", "Soyad", "TC", "Telefon", "Departman", "Pozisyon"};
        return excel("Personel", kolonlar, rows);
    }

    /** Maaş kolonu yalnızca ADMIN ve MUHASEBE rollerine eklenir. */
    private boolean maasGorebilir() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(r -> "ROLE_ADMIN".equals(r) || "ROLE_MUHASEBE".equals(r));
    }

    @GetMapping("/izinler")
    @Operation(summary = "İzinleri Excel dışa aktar", description = "Personel izinlerini (durum/personel/tarih/arama filtreleriyle) Excel (.xlsx) olarak dışa aktarır")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IK_EXPORT')")
    public ResponseEntity<byte[]> izinler(
            HttpServletRequest req,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) Long personelId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate baslangic,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bitis,
            @RequestParam(required = false) String q) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = personelIzinService.filtreli(sirketId, durum, personelId, baslangic, bitis, q, MAX_EXPORT_PAGE).getContent();
        var rows = list.stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", i.getId()); m.put("Personel", i.getPersonelAdi());
            m.put("İzin Türü", i.getIzinTuru()); m.put("Başlangıç", i.getBaslangic());
            m.put("Bitiş", i.getBitis()); m.put("Gün", i.getGunSayisi());
            m.put("Durum", i.getDurum()); m.put("Açıklama", i.getAciklama());
            m.put("Onaylayan", i.getOnaylayan());
            return m;
        }).toList();
        return excel("Izinler", new String[]{"ID", "Personel", "İzin Türü", "Başlangıç", "Bitiş", "Gün", "Durum", "Açıklama", "Onaylayan"}, rows);
    }

    @GetMapping("/puantaj")
    @Operation(summary = "Puantajı Excel dışa aktar", description = "Şirket geneli puantaj kayıtlarını (personel/tarih/durum/arama filtreleriyle) Excel (.xlsx) olarak dışa aktarır")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IK_EXPORT')")
    public ResponseEntity<byte[]> puantaj(
            HttpServletRequest req,
            @RequestParam(required = false) Long personelId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate baslangic,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bitis,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) String q) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = personelPuantajService.filtreli(sirketId, personelId, baslangic, bitis, durum, q, MAX_EXPORT_PAGE).getContent();
        var rows = list.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", p.getId()); m.put("Personel", p.getPersonelAdi());
            m.put("Tarih", p.getTarih()); m.put("Durum", p.getDurum());
            m.put("Açıklama", p.getAciklama());
            return m;
        }).toList();
        return excel("Puantaj", new String[]{"ID", "Personel", "Tarih", "Durum", "Açıklama"}, rows);
    }

    @GetMapping("/vardiyalar")
    @Operation(summary = "Vardiyaları Excel dışa aktar", description = "Vardiya kayıtlarını (personel/tarih/tür/arama filtreleriyle) Excel (.xlsx) olarak dışa aktarır")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'IK_EXPORT')")
    public ResponseEntity<byte[]> vardiyalar(
            HttpServletRequest req,
            @RequestParam(required = false) Long personelId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate baslangic,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bitis,
            @RequestParam(required = false) String tur,
            @RequestParam(required = false) String q) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = vardiyaService.filtreli(sirketId, personelId, baslangic, bitis, tur, q, MAX_EXPORT_PAGE).getContent();
        var rows = list.stream().map(v -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", v.getId()); m.put("Personel", v.getPersonelAdi());
            m.put("Tarih", v.getTarih()); m.put("Başlangıç", v.getBaslangic());
            m.put("Bitiş", v.getBitis()); m.put("Tür", v.getTur());
            return m;
        }).toList();
        return excel("Vardiyalar", new String[]{"ID", "Personel", "Tarih", "Başlangıç", "Bitiş", "Tür"}, rows);
    }

    @GetMapping("/bankalar")
    @Operation(summary = "Bankaları Excel dışa aktar", description = "Bankaları Excel (.xlsx) dosyası olarak dışa aktarır")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'FINANS_EXPORT')")
    public ResponseEntity<byte[]> bankalar(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = bankaService.tumBankalariGetir(sirketId, MAX_EXPORT_PAGE).getContent();
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
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'FINANS_EXPORT')")
    public ResponseEntity<byte[]> kasalar(HttpServletRequest req) {
        Long sirketId = (Long) req.getAttribute("sirketId");
        var list = kasaService.tumKasalarGetir(sirketId, MAX_EXPORT_PAGE).getContent();
        var rows = list.stream().map(k -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", k.getId()); m.put("Ad", k.getAd()); m.put("Bakiye", k.getBakiye());
            return m;
        }).toList();
        return excel("Kasalar", new String[]{"ID", "Ad", "Bakiye"}, rows);
    }

    @GetMapping("/denetim-log")
    @Operation(summary = "Denetim loglarını Excel dışa aktar", description = "Denetim log kayıtlarını Excel (.xlsx) dosyası olarak dışa aktarır")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'SISTEM_EXPORT')")
    public ResponseEntity<byte[]> denetimLog(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam(required = false) Long kullaniciId,
            @RequestParam(required = false) String islem,
            @RequestParam(required = false) String entityAdi,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) Boolean riskli,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate baslangicTarih,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bitisTarih) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var list = auditLogService.filtreliGetir(sirketId, kullaniciId, islem, entityAdi, entityId, riskli, q, baslangicTarih, bitisTarih, MAX_EXPORT_PAGE).getContent();
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
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'RAPOR_EXPORT')")
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
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'RAPOR_EXPORT')")
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

    @GetMapping("/siparis-takip")
    @Operation(summary = "Sipariş takip zincirini Excel dışa aktar", description = "Filtrelenen sipariş üretim→sevk→teslimat zincirini Excel olarak dışa aktarır")
    public ResponseEntity<byte[]> siparisTakip(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) String sofor) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var list = siparisTakipService.filtreliZincir(sirketId, q, durum, sofor, false, false, MAX_EXPORT_PAGE).getContent();
        List<Map<String, Object>> rows = list.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("Sipariş No", s.getSiparisNo());
            m.put("Cari", s.getCariAd());
            m.put("Sipariş Durum", s.getSiparisDurum());
            m.put("Şoför", s.getDriverAd());
            m.put("Üretim Durum", s.getUretimDurum());
            m.put("Üretim Adet", s.getUretimSayisi());
            m.put("Sevk Durum", s.getSevkDurum());
            m.put("Sevk Adet", s.getSevkSayisi());
            m.put("Teslimat Durum", s.getTeslimatDurum());
            m.put("Beklenen Teslim", s.getBeklenenTeslimTarihi());
            m.put("Gecikti", s.isTeslimatGecikti() ? "Evet" : "Hayır");
            return m;
        }).toList();
        return excel("SiparisTakip", new String[]{"Sipariş No", "Cari", "Sipariş Durum", "Şoför",
                "Üretim Durum", "Üretim Adet", "Sevk Durum", "Sevk Adet", "Teslimat Durum",
                "Beklenen Teslim", "Gecikti"}, rows);
    }

    @GetMapping("/teslimatlar")
    @Operation(summary = "Teslimatları Excel dışa aktar", description = "Teslimat kayıtlarını Excel (.xlsx) olarak dışa aktarır (opsiyonel driverId).")
    public ResponseEntity<byte[]> teslimatlar(
            HttpServletRequest request,
            @RequestParam(required = false) Long driverId) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        var list = teslimatService.teslimatlar(sirketId, driverId, kullaniciId);
        List<Map<String, Object>> rows = list.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("Fatura", t.getFaturaNumarasi());
            m.put("Müşteri", t.getMusteriAdi());
            m.put("Adres", t.getTeslimatAdresi());
            m.put("Durum", t.getDurum());
            m.put("Beklenen Teslim", t.getBeklenenTeslimTarihi());
            m.put("Şoför", t.getTeslimEdenAd());
            m.put("Teslim Alan", t.getTeslimAlanAd());
            m.put("Teslim Tarihi", t.getTeslimTarihi());
            return m;
        }).toList();
        return excel("Teslimatlar", new String[]{"Fatura", "Müşteri", "Adres", "Durum",
                "Beklenen Teslim", "Şoför", "Teslim Alan", "Teslim Tarihi"}, rows);
    }

    @GetMapping("/uretim-emirleri")
    @Operation(summary = "Üretim emirlerini Excel dışa aktar", description = "Filtrelenen üretim emirlerini Excel (.xlsx) olarak dışa aktarır.")
    public ResponseEntity<byte[]> uretimEmirleri(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) String oncelik,
            @RequestParam(required = false) Boolean geciken,
            @RequestParam(required = false) Boolean buAy,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate baslangic,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var page = uretimService.filtreliEmirler(sirketId, q, durum, oncelik, geciken, buAy, baslangic, bitis,
                PageRequest.of(0, MAX_EXPORT_ROWS, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "olusturmaTarihi")));
        List<Map<String, Object>> rows = page.getContent().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", e.getId());
            m.put("Mamul", e.getUrunAd());
            m.put("Miktar", e.getMiktar());
            m.put("Durum", e.getDurum());
            m.put("Öncelik", e.getOncelik());
            m.put("Planlanan Başlangıç", e.getPlanlananBaslangic());
            m.put("Planlanan Bitiş", e.getPlanlananBitis());
            m.put("Üretilen", e.getUretilenMiktar());
            m.put("Fire", e.getFireMiktar());
            m.put("Toplam Maliyet", e.getToplamMaliyet());
            m.put("Oluşturma", e.getOlusturmaTarihi());
            return m;
        }).toList();
        return excel("UretimEmirleri", new String[]{"ID", "Mamul", "Miktar", "Durum", "Öncelik",
                "Planlanan Başlangıç", "Planlanan Bitiş", "Üretilen", "Fire", "Toplam Maliyet", "Oluşturma"}, rows);
    }

    @GetMapping("/uretim-receteler")
    @Operation(summary = "Üretim reçetelerini Excel dışa aktar", description = "Reçeteleri (ürün ağacı) Excel (.xlsx) olarak dışa aktarır.")
    public ResponseEntity<byte[]> uretimReceteler(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var list = uretimService.receteler(sirketId);
        List<Map<String, Object>> rows = list.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", r.getId());
            m.put("Reçete", r.getAd());
            m.put("Mamul", r.getUrunAd());
            m.put("Kalem Sayısı", r.getKalemler() != null ? r.getKalemler().size() : 0);
            m.put("Fire Oranı", r.getFireOrani());
            m.put("Revizyon", r.getRevizyon());
            m.put("Aktif", Boolean.FALSE.equals(r.getAktif()) ? "Hayır" : "Evet");
            return m;
        }).toList();
        return excel("UretimReceteler", new String[]{"ID", "Reçete", "Mamul", "Kalem Sayısı",
                "Fire Oranı", "Revizyon", "Aktif"}, rows);
    }

    @GetMapping("/gorunum360-stok-kar")
    @Operation(summary = "360 Stok Kâr görünümünü Excel dışa aktar",
            description = "Ürün bazında ciro, maliyet, brüt kâr ve marj tablosunu Excel (.xlsx) olarak dışa aktarır.")
    public ResponseEntity<byte[]> gorunum360StokKar(
            HttpServletRequest request,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate baslangic,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var veri = gorunum360Service.stokKar360(sirketId, baslangic, bitis);
        List<Map<String, Object>> rows = veri.getUrunler().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("Ürün", u.getAd());
            m.put("Stok Kodu", u.getStokKodu());
            m.put("Satış Adet", u.getSatisAdet());
            m.put("Ciro", u.getCiro());
            m.put("Maliyet", u.getMaliyet());
            m.put("Brüt Kâr", u.getBrutKar());
            m.put("Marj %", u.getMarj());
            m.put("Stok", u.getStokMiktar());
            return m;
        }).toList();
        return excel("Gorunum360-StokKar", new String[]{"Ürün", "Stok Kodu", "Satış Adet", "Ciro",
                "Maliyet", "Brüt Kâr", "Marj %", "Stok"}, rows);
    }

    @GetMapping("/gorunum360-calisan")
    @Operation(summary = "360 Çalışan performansını Excel dışa aktar",
            description = "Şoför bazında toplam/tamamlanan/bekleyen teslimat ve teslim tutarını Excel (.xlsx) olarak dışa aktarır.")
    public ResponseEntity<byte[]> gorunum360Calisan(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var veri = gorunum360Service.calisanPerformans360(sirketId);
        List<Map<String, Object>> rows = veri.getCalisanlar().stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("Çalışan", c.getAd());
            m.put("Rol", c.getRol());
            m.put("Toplam Teslimat", c.getToplamTeslimat());
            m.put("Tamamlanan", c.getTamamlananTeslimat());
            m.put("Bekleyen", c.getBekleyenTeslimat());
            m.put("Teslim Tutarı", c.getTeslimTutari());
            return m;
        }).toList();
        return excel("Gorunum360-Calisan", new String[]{"Çalışan", "Rol", "Toplam Teslimat",
                "Tamamlanan", "Bekleyen", "Teslim Tutarı"}, rows);
    }

    @GetMapping("/gorunum360-segment")
    @Operation(summary = "360 Müşteri segmentasyonunu Excel dışa aktar",
            description = "Müşteri segmentlerini (gerekçe, ciro, kalan, fatura adet) Excel (.xlsx) olarak dışa aktarır.")
    public ResponseEntity<byte[]> gorunum360Segment(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var veri = gorunum360Service.musteriSegmentasyon(sirketId);
        List<Map<String, Object>> rows = veri.getMusteriler().stream().map(m -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("Müşteri", m.getAd());
            r.put("Segment", m.getSegment());
            r.put("Gerekçe", m.getGerekce());
            r.put("Ciro", m.getCiro());
            r.put("Kalan", m.getKalanTutar());
            r.put("Fatura Adet", m.getFaturaAdet());
            r.put("Son Fatura (gün)", m.getSonFaturaGunOnce());
            return r;
        }).toList();
        return excel("Gorunum360-Segment", new String[]{"Müşteri", "Segment", "Gerekçe", "Ciro",
                "Kalan", "Fatura Adet", "Son Fatura (gün)"}, rows);
    }

    @GetMapping("/donemler")
    @Operation(summary = "Dönemleri Excel dışa aktar",
            description = "Seçili şirketin mali dönemlerini (tarih aralığı, aktif/kilitli, kilit bilgisi) Excel (.xlsx) olarak dışa aktarır.")
    public ResponseEntity<byte[]> donemler(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var list = donemService.sirketeGoreGetir(sirketId);
        List<Map<String, Object>> rows = list.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", d.getId());
            m.put("Dönem", d.getAd());
            m.put("Başlangıç", d.getBaslangic());
            m.put("Bitiş", d.getBitis());
            m.put("Aktif", Boolean.TRUE.equals(d.getAktif()) ? "Evet" : "Hayır");
            m.put("Kilitli", Boolean.TRUE.equals(d.getKilitli()) ? "Evet" : "Hayır");
            m.put("Kilit Tarihi", d.getKilitTarihi());
            m.put("Oluşturma", d.getOlusturmaTarihi());
            return m;
        }).toList();
        return excel("Donemler", new String[]{"ID", "Dönem", "Başlangıç", "Bitiş", "Aktif",
                "Kilitli", "Kilit Tarihi", "Oluşturma"}, rows);
    }

    @GetMapping("/donem-kapanislar")
    @Operation(summary = "Dönem kapanışlarını Excel dışa aktar",
            description = "Yıl sonu kapanış kayıtlarını (mali yıl, tarih, kullanıcı, kilitlenen dönem, not) Excel (.xlsx) olarak dışa aktarır.")
    public ResponseEntity<byte[]> donemKapanislar(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var list = donemService.kapanislariGetir(sirketId);
        List<Map<String, Object>> rows = list.stream().map(k -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", k.getId());
            m.put("Mali Yıl", k.getYil());
            m.put("Kapanış Tarihi", k.getKapanisTarihi());
            m.put("Kullanıcı ID", k.getKullaniciId());
            m.put("Kilitlenen Dönem", k.getKilitlenenDonemSayisi());
            m.put("Not", k.getOzet());
            return m;
        }).toList();
        return excel("DonemKapanislar", new String[]{"ID", "Mali Yıl", "Kapanış Tarihi",
                "Kullanıcı ID", "Kilitlenen Dönem", "Not"}, rows);
    }

    @GetMapping("/stok-sayim")
    @Operation(summary = "Stok sayımlarını Excel dışa aktar",
            description = "Filtrelenen stok sayımlarını (tarih, ürün, beklenen/sayılan/fark, durum) Excel (.xlsx) olarak dışa aktarır.")
    public ResponseEntity<byte[]> stokSayim(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate baslangic,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var list = stokSayimService.filtreliGetir(sirketId, q, durum, baslangic, bitis, MAX_EXPORT_PAGE).getContent();
        List<Map<String, Object>> rows = list.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", s.getId());
            m.put("Tarih", s.getTarih());
            m.put("Ürün", s.getStokAdi());
            m.put("Beklenen", s.getBeklenenMiktar());
            m.put("Sayılan", s.getSayilanMiktar());
            m.put("Fark", s.getFark());
            m.put("Durum", s.getDurum());
            m.put("Açıklama", s.getAciklama());
            return m;
        }).toList();
        return excel("StokSayim", new String[]{"ID", "Tarih", "Ürün", "Beklenen", "Sayılan",
                "Fark", "Durum", "Açıklama"}, rows);
    }

    @GetMapping("/stok-seri")
    @Operation(summary = "Seri/lotları Excel dışa aktar",
            description = "Filtrelenen seri/lot kayıtlarını (ürün, seri/lot, miktar/kalan, SKT, durum) Excel (.xlsx) olarak dışa aktarır.")
    public ResponseEntity<byte[]> stokSeri(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) Long depoId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate sktBaslangic,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate sktBitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var list = stokSeriService.filtreliGetir(sirketId, q, durum, depoId, sktBaslangic, sktBitis, MAX_EXPORT_PAGE)
                .getContent();
        List<Map<String, Object>> rows = list.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ID", s.getId());
            m.put("Ürün", s.getStokAdi());
            m.put("Seri No", s.getSeriNo());
            m.put("Lot No", s.getLotNo());
            m.put("Miktar", s.getMiktar());
            m.put("Kalan", s.getKalanMiktar());
            m.put("Depo ID", s.getDepoId());
            m.put("SKT", s.getSonKullanmaTarihi());
            m.put("Durum", s.getDurum());
            m.put("Giriş", s.getGirisTarihi());
            return m;
        }).toList();
        return excel("StokSeriLot", new String[]{"ID", "Ürün", "Seri No", "Lot No", "Miktar",
                "Kalan", "Depo ID", "SKT", "Durum", "Giriş"}, rows);
    }

    @GetMapping("/anomaliler")
    @Operation(summary = "Anomalileri Excel dışa aktar",
            description = "Taranan anomalileri (tür, seviye, başlık, açıklama, öneri) Excel (.xlsx) olarak dışa aktarır.")
    public ResponseEntity<byte[]> anomaliler(
            HttpServletRequest request,
            @RequestParam(required = false) String seviye,
            @RequestParam(required = false) String tur,
            @RequestParam(required = false) String q) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        String qq = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(java.util.Locale.ROOT);
        java.util.Set<String> seviyeler = null;
        if (seviye != null && !seviye.isBlank()) {
            seviyeler = new java.util.HashSet<>();
            for (String s : seviye.split(",")) {
                if (!s.isBlank()) seviyeler.add(s.trim());
            }
        }
        final java.util.Set<String> seviyeFiltre = seviyeler;
        List<Map<String, Object>> rows = anomaliTespitEngine.anomalileriTara(sirketId).stream()
                .filter(a -> seviyeFiltre == null || seviyeFiltre.contains(a.getSeviye()))
                .filter(a -> tur == null || tur.isBlank() || tur.equals(a.getTur()))
                .filter(a -> qq == null
                        || (a.getBaslik() != null && a.getBaslik().toLowerCase(java.util.Locale.ROOT).contains(qq))
                        || (a.getAciklama() != null && a.getAciklama().toLowerCase(java.util.Locale.ROOT).contains(qq))
                        || (a.getOneri() != null && a.getOneri().toLowerCase(java.util.Locale.ROOT).contains(qq)))
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("Tür", a.getTur());
                    m.put("Seviye", a.getSeviye());
                    m.put("Başlık", a.getBaslik());
                    m.put("Açıklama", a.getAciklama());
                    m.put("İlgili Kayıt", a.getIlgiliKayitId());
                    m.put("Öneri", a.getOneri());
                    m.put("Tespit Tarihi", a.getTespitTarihi());
                    return m;
                }).toList();
        return excel("Anomaliler", new String[]{"Tür", "Seviye", "Başlık", "Açıklama",
                "İlgili Kayıt", "Öneri", "Tespit Tarihi"}, rows);
    }

    @GetMapping("/ip-whitelist")
    @Operation(summary = "IP beyaz listesini Excel dışa aktar",
            description = "Güvenli IP/CIDR tanımlarını Excel (.xlsx) olarak dışa aktarır (yalnızca ADMIN).")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> ipWhitelist(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        List<Map<String, Object>> rows = ipWhitelistService.listele(sirketId).stream().map(k -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("IP/CIDR", k.get("ipAdresi"));
            m.put("Açıklama", k.get("aciklama"));
            m.put("Durum", k.get("durum"));
            m.put("Ekleme Tarihi", k.get("eklemeTarihi"));
            return m;
        }).toList();
        return excel("IpWhitelist", new String[]{"IP/CIDR", "Açıklama", "Durum", "Ekleme Tarihi"}, rows);
    }

    @GetMapping("/subeler")
    @Operation(summary = "Şubeleri Excel dışa aktar",
            description = "Şubeleri (yetkili, telefon, adres, durum) Excel (.xlsx) olarak dışa aktarır; opsiyonel durum filtresi.")
    public ResponseEntity<byte[]> subeler(
            HttpServletRequest request,
            @RequestParam(required = false) String durum) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var list = subeService.tumunuGetir(sirketId, MAX_EXPORT_PAGE).getContent();
        List<Map<String, Object>> rows = list.stream()
                .filter(s -> durum == null || durum.isBlank()
                        || ("AKTIF".equalsIgnoreCase(durum) && Boolean.TRUE.equals(s.getAktif()))
                        || ("PASIF".equalsIgnoreCase(durum) && !Boolean.TRUE.equals(s.getAktif())))
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("ID", s.getId());
                    m.put("Şube", s.getAd());
                    m.put("Yetkili", s.getYetkili());
                    m.put("Telefon", s.getTelefon());
                    m.put("Adres", s.getAdres());
                    m.put("Durum", Boolean.FALSE.equals(s.getAktif()) ? "Pasif" : "Aktif");
                    m.put("Oluşturma", s.getOlusturmaTarihi());
                    return m;
                }).toList();
        return excel("Subeler", new String[]{"ID", "Şube", "Yetkili", "Telefon", "Adres",
                "Durum", "Oluşturma"}, rows);
    }

    private ResponseEntity<byte[]> excel(String name, String[] cols, List<Map<String, Object>> rows) {
        boolean kesildi = rows.size() >= MAX_EXPORT_ROWS;
        List<Map<String, Object>> hedef = rows;
        if (kesildi) {
            log.warn("Excel dışa aktarım satır limitine ulaşıldı ({}); çıktı kesildi. Dosya: {}", MAX_EXPORT_ROWS, name);
            // Kullanıcı eksik dosyayı tam sanmasın: son satıra görünür uyarı eklenir.
            hedef = new ArrayList<>(rows);
            Map<String, Object> uyari = new LinkedHashMap<>();
            for (String c : cols) uyari.put(c, "");
            uyari.put(cols[0], "UYARI: " + MAX_EXPORT_ROWS
                    + " kayıt sınırı nedeniyle liste kesildi. Lütfen filtreleyip tekrar deneyin.");
            hedef.add(uyari);
        }
        byte[] data = excelService.export(name, cols, hedef);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + guvenliDosyaAdi(name) + "-" + java.time.LocalDate.now() + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        if (kesildi) {
            builder.header("X-Export-Truncated", "true");
        }
        return builder.body(data);
    }

    /** Content-Disposition başlığına kullanıcı girdisi gömülürken başlık bozulmasını engeller. */
    private String guvenliDosyaAdi(String name) {
        if (name == null) return "export";
        String s = name.replaceAll("[\\r\\n\"\\\\/;]", "_").trim();
        if (s.isBlank()) return "export";
        return s.length() > 60 ? s.substring(0, 60) : s;
    }
}
