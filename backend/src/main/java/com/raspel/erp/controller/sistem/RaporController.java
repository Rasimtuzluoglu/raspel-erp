package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.RaporDTO;
import com.raspel.erp.service.sistem.RaporService;
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
import com.raspel.erp.entity.sistem.Donem;

@Tag(name = "Raporlar", description = "Raporlama API")
@RestController
@RequestMapping("/api/raporlar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class RaporController {

    private final RaporService raporService;

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
}