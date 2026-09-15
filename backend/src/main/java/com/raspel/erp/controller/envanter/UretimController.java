package com.raspel.erp.controller.envanter;

import com.raspel.erp.dto.envanter.ReceteDTO;
import com.raspel.erp.dto.envanter.UretimEmriDTO;
import com.raspel.erp.dto.envanter.UretimEmriLogDTO;
import com.raspel.erp.dto.envanter.UretimIhtiyacDTO;
import com.raspel.erp.dto.envanter.UretimOzetDTO;
import com.raspel.erp.dto.envanter.UretimTamamlaIstek;
import com.raspel.erp.dto.ticaret.SatinalmaTalepDTO;
import com.raspel.erp.service.envanter.UretimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Üretim", description = "Reçete (ürün ağacı) ve üretim emri yönetimi API")
@RestController
@RequestMapping("/api/uretim")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class UretimController {

    private final UretimService uretimService;

    private Long sirket(HttpServletRequest request) {
        return (Long) request.getAttribute("sirketId");
    }

    private Long kullanici(HttpServletRequest request) {
        return (Long) request.getAttribute("kullaniciId");
    }

    // ---------- Reçeteler ----------

    @GetMapping("/receteler")
    @Operation(summary = "Reçeteleri listele")
    public ResponseEntity<List<ReceteDTO>> receteler(HttpServletRequest request) {
        return ResponseEntity.ok(uretimService.receteler(sirket(request)));
    }

    @PostMapping("/receteler")
    @Operation(summary = "Reçete oluştur")
    public ResponseEntity<ReceteDTO> receteOlustur(@RequestBody ReceteDTO dto, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(uretimService.receteOlustur(dto, sirket(request)));
    }

    @PutMapping("/receteler/{id}")
    @Operation(summary = "Reçete güncelle (revizyon artar)")
    public ResponseEntity<ReceteDTO> receteGuncelle(@PathVariable Long id, @RequestBody ReceteDTO dto, HttpServletRequest request) {
        return ResponseEntity.ok(uretimService.receteGuncelle(id, dto, sirket(request)));
    }

    @DeleteMapping("/receteler/{id}")
    @Operation(summary = "Reçete sil")
    public ResponseEntity<Void> receteSil(@PathVariable Long id, HttpServletRequest request) {
        uretimService.receteSil(id, sirket(request));
        return ResponseEntity.noContent().build();
    }

    // ---------- Emirler ----------

    @GetMapping("/emirler")
    @Operation(summary = "Üretim emirlerini listele")
    public ResponseEntity<List<UretimEmriDTO>> emirler(HttpServletRequest request) {
        return ResponseEntity.ok(uretimService.uretimEmirleri(sirket(request)));
    }

    @PostMapping("/emirler")
    @Operation(summary = "Üretim emri oluştur")
    public ResponseEntity<UretimEmriDTO> emirOlustur(@RequestBody UretimEmriDTO dto, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(uretimService.uretimEmriOlustur(dto, sirket(request)));
    }

    @PostMapping("/emirler/{id}/baslat")
    @Operation(summary = "Üretim emrini başlat (TASLAK -> URETIMDE)")
    public ResponseEntity<UretimEmriDTO> baslat(@PathVariable Long id, HttpServletRequest request) {
        return ResponseEntity.ok(uretimService.emirBaslat(id, sirket(request), kullanici(request)));
    }

    @PostMapping("/emirler/{id}/tamamla")
    @Operation(summary = "Üretim emrini tamamla", description = "Kısmi üretim ve fire destekler; hammadde tüketir, mamul üretir, maliyet hesaplar")
    public ResponseEntity<UretimEmriDTO> tamamla(@PathVariable Long id,
                                                 @RequestBody(required = false) UretimTamamlaIstek istek,
                                                 HttpServletRequest request) {
        return ResponseEntity.ok(uretimService.emirTamamla(id, sirket(request), istek, kullanici(request)));
    }

    @PostMapping("/emirler/{id}/iptal")
    @Operation(summary = "Üretim emrini iptal et")
    public ResponseEntity<UretimEmriDTO> iptal(@PathVariable Long id,
                                               @RequestBody(required = false) IptalIstek istek,
                                               HttpServletRequest request) {
        return ResponseEntity.ok(uretimService.emirIptal(id, sirket(request),
                istek != null ? istek.aciklama() : null, kullanici(request)));
    }

    @GetMapping("/emirler/{id}/gecmis")
    @Operation(summary = "Üretim emri durum geçmişi")
    public ResponseEntity<List<UretimEmriLogDTO>> gecmis(@PathVariable Long id, HttpServletRequest request) {
        return ResponseEntity.ok(uretimService.emirGecmisi(id, sirket(request)));
    }

    @PostMapping("/emirler/{id}/satinalma-talebi")
    @Operation(summary = "Eksik hammaddeler için satınalma talebi oluştur")
    public ResponseEntity<SatinalmaTalepDTO> satinalmaTalebi(@PathVariable Long id, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(uretimService.ihtiyactanSatinalmaTalebi(id, sirket(request), null));
    }

    @PostMapping("/emirler/siparisten/{siparisId}")
    @Operation(summary = "Siparişten üretim emri(leri) oluştur")
    public ResponseEntity<List<UretimEmriDTO>> siparistenEmir(@PathVariable Long siparisId, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(uretimService.siparistenEmirOlustur(siparisId, sirket(request)));
    }

    // ---------- Analiz / Özet ----------

    @GetMapping("/ihtiyac")
    @Operation(summary = "Hammadde ihtiyaç analizi", description = "Ürün + miktar için gerekli/mevcut/eksik hammadde ve maliyet")
    public ResponseEntity<UretimIhtiyacDTO> ihtiyac(@RequestParam Long urunId,
                                                    @RequestParam BigDecimal miktar,
                                                    HttpServletRequest request) {
        return ResponseEntity.ok(uretimService.ihtiyacAnalizi(urunId, miktar, sirket(request)));
    }

    @GetMapping("/ozet")
    @Operation(summary = "Üretim KPI özeti")
    public ResponseEntity<UretimOzetDTO> ozet(HttpServletRequest request) {
        return ResponseEntity.ok(uretimService.ozet(sirket(request)));
    }

    record IptalIstek(String aciklama) {}
}
