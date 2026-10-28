package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.CekSenetAciklamaRequest;
import com.raspel.erp.dto.finans.CekSenetCiroRequest;
import com.raspel.erp.dto.finans.CekSenetDTO;
import com.raspel.erp.dto.finans.CekSenetOzetDTO;
import com.raspel.erp.dto.finans.CekSenetTahsilIptalRequest;
import com.raspel.erp.dto.finans.CekSenetTakvimDTO;
import com.raspel.erp.service.finans.CekSenetService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Çek/Senet", description = "Çek ve senet yönetimi API")
@RestController
@RequestMapping("/api/cek-senet")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
public class CekSenetController {

    private final CekSenetService cekSenetService;

    @GetMapping
    @Operation(summary = "Tüm çek/senetleri getir", description = "Tüm çek ve senetleri listeler")
    public ResponseEntity<Page<CekSenetDTO>> tumu(HttpServletRequest request,
                                                  @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(cekSenetService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/filtreli")
    @Operation(summary = "Filtreli çek/senet listesi",
            description = "Arama, tür, durum, cari, vade tarihi/aralığı, min tutar ve protesto filtreleriyle sayfalı liste")
    public ResponseEntity<Page<CekSenetDTO>> filtreli(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tur,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) Long cariId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @RequestParam(required = false) String vadeAralik,
            @RequestParam(required = false) BigDecimal minTutar,
            @RequestParam(required = false) Boolean protesto,
            @PageableDefault(size = 50, sort = "vadeTarihi", direction = Sort.Direction.ASC) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(cekSenetService.filtreli(sirketId, q, tur, durum, cariId,
                baslangic, bitis, vadeAralik, minTutar, protesto, pageable));
    }

    @GetMapping("/ozet")
    @Operation(summary = "Çek/senet KPI özeti",
            description = "Durum bazlı sayımlar, vadesi geçen/bu ay vadesi gelen ve toplam tutar")
    public ResponseEntity<CekSenetOzetDTO> ozet(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(cekSenetService.ozet(sirketId, baslangic, bitis));
    }

    @GetMapping("/takvim")
    @Operation(summary = "Vade takvimi",
            description = "Gün bazlı vade takvimi (en fazla 62 gün); kayıt olmayan günler adet=0 ile döner")
    public ResponseEntity<List<CekSenetTakvimDTO>> takvim(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(cekSenetService.takvim(sirketId, baslangic, bitis));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre çek/senet getir",
            description = "Çek/senet detayını (cari/fatura/ciro/kasa-banka bilgileri ve durum geçmişi) getirir")
    public ResponseEntity<CekSenetDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(cekSenetService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni çek/senet oluştur", description = "Yeni bir çek veya senet oluşturur")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<CekSenetDTO> olustur(@Valid @RequestBody CekSenetDTO dto, HttpServletRequest request) {
        dto.setSirketId((Long) request.getAttribute("sirketId"));
        return ResponseEntity.status(HttpStatus.CREATED).body(cekSenetService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Çek/senet güncelle", description = "Çek/senet bilgilerini günceller")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<CekSenetDTO> guncelle(@PathVariable Long id, @Valid @RequestBody CekSenetDTO dto) {
        return ResponseEntity.ok(cekSenetService.guncelle(id, dto));
    }

    /** Geriye uyum için korunur; yeni aksiyon uçları tercih edilir. */
    @PutMapping("/{id}/durum")
    @Operation(summary = "Çek/senet durum güncelle", description = "Çek/senet durumunu günceller (legacy uç)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<CekSenetDTO> durumGuncelle(@PathVariable Long id,
            @RequestBody @Valid com.raspel.erp.dto.sistem.DurumGuncelleRequest body) {
        return ResponseEntity.ok(cekSenetService.durumGuncelle(id, body.getDurum(),
                body.getKasaId(), body.getBankaId(), body.getFaturaId()));
    }

    @PostMapping("/{id}/tahsil-iptal")
    @Operation(summary = "Tahsil iptali",
            description = "Tahsil edilmiş çek/senedi gerekçeyle geri alır; cari/kasa/banka etkileri ters kaydedilir")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<CekSenetDTO> tahsilIptal(@PathVariable Long id,
            @Valid @RequestBody CekSenetTahsilIptalRequest body) {
        return ResponseEntity.ok(cekSenetService.tahsilIptal(id, body.getGerekce()));
    }

    @PostMapping("/{id}/ciro")
    @Operation(summary = "Çek/senedi ciro et",
            description = "Portföydeki çek/senedi bir cariye ciro eder (finansal hareket oluşmaz)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<CekSenetDTO> ciro(@PathVariable Long id,
            @Valid @RequestBody CekSenetCiroRequest body) {
        return ResponseEntity.ok(cekSenetService.ciroEt(id, body.getCariId(), body.getNot()));
    }

    @PostMapping("/{id}/ciro-iade")
    @Operation(summary = "Ciro iadesi", description = "Ciro edilmiş çek/senedi iade durumuna alır")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<CekSenetDTO> ciroIade(@PathVariable Long id) {
        return ResponseEntity.ok(cekSenetService.ciroIade(id));
    }

    @PostMapping("/{id}/iade-al")
    @Operation(summary = "Portföye geri al", description = "İade durumundaki çek/senedi portföye geri alır")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<CekSenetDTO> iadeAl(@PathVariable Long id) {
        return ResponseEntity.ok(cekSenetService.iadeAl(id));
    }

    @PostMapping("/{id}/teminat")
    @Operation(summary = "Teminat ver", description = "Portföydeki çek/senedi teminata verir")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<CekSenetDTO> teminat(@PathVariable Long id,
            @Valid @RequestBody(required = false) CekSenetAciklamaRequest body) {
        return ResponseEntity.ok(cekSenetService.teminatVer(id, body != null ? body.getAciklama() : null));
    }

    @PostMapping("/{id}/teminat-iade")
    @Operation(summary = "Teminat iadesi", description = "Teminattaki çek/senedi portföye döndürür")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<CekSenetDTO> teminatIade(@PathVariable Long id) {
        return ResponseEntity.ok(cekSenetService.teminatIade(id));
    }

    @PostMapping("/{id}/protesto")
    @Operation(summary = "Protesto", description = "Çek/senedi protesto eder (terminal durum)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUHASEBE')")
    public ResponseEntity<CekSenetDTO> protesto(@PathVariable Long id,
            @Valid @RequestBody(required = false) CekSenetAciklamaRequest body) {
        return ResponseEntity.ok(cekSenetService.protesto(id, body != null ? body.getAciklama() : null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Çek/senet sil", description = "Çek/senedi siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        cekSenetService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
