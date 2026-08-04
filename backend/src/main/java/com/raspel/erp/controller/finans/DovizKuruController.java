package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.DovizKuruDTO;
import com.raspel.erp.entity.finans.DovizKuru;
import com.raspel.erp.service.finans.DovizKuruService;
import com.raspel.erp.service.sistem.TcmbKurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Döviz Kurları", description = "TCMB güncel ve günlük döviz/altın kurları API")
@RestController
@RequestMapping({"/api/doviz-kurlari", "/api/doviz"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class DovizKuruController {

    private final DovizKuruService dovizKuruService;
    private final TcmbKurService tcmbKurService;

    @GetMapping
    @Operation(summary = "Günlük döviz kurlarını getir", description = "Tarihe göre döviz kurlarını listeler (Varsayılan: Bugüne ait kurlar, TCMB'den tazelenir)")
    public ResponseEntity<List<DovizKuruDTO>> gunlukKurlariGetir(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tarih) {
        return ResponseEntity.ok(dovizKuruService.gunlukKurlariGetir(tarih));
    }

    @GetMapping("/kurlar")
    @Operation(summary = "Tüm güncel kurları getir", description = "TCMB'den USD, EUR, GBP, SAR ve Gram Altın kurlarını listeler")
    public ResponseEntity<List<DovizKuru>> tumKurlar() {
        return ResponseEntity.ok(tcmbKurService.tumKurlariGetir());
    }

    @PostMapping("/guncelle")
    @Operation(summary = "TCMB kurlarını yenile", description = "TCMB servisinden anlık kurları yeniden çeker")
    public ResponseEntity<List<DovizKuru>> guncelle() {
        tcmbKurService.tcmbKurlariniGuncelle();
        return ResponseEntity.ok(tcmbKurService.tumKurlariGetir());
    }

    @GetMapping("/cevir")
    @Operation(summary = "Para birimi dönüştür", description = "Belirtilen kaynak para birimini hedef para birimine dönüştürür")
    public ResponseEntity<Map<String, Object>> cevir(
            @RequestParam BigDecimal tutar,
            @RequestParam(defaultValue = "TRY") String kaynak,
            @RequestParam(defaultValue = "USD") String hedef) {
        BigDecimal sonuc = tcmbKurService.cevir(tutar, kaynak, hedef);
        return ResponseEntity.ok(Map.of(
                "tutar", tutar,
                "kaynak", kaynak,
                "hedef", hedef,
                "sonuc", sonuc
        ));
    }

    @PostMapping
    @Operation(summary = "Döviz kuru ekle veya güncelle", description = "Belirtilen tarihe ait döviz kurunu kaydeder (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DovizKuruDTO> kurKaydet(@Valid @RequestBody DovizKuruDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dovizKuruService.kurEkleVeyaGuncelle(dto));
    }
}
