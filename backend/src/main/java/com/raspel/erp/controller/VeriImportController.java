package com.raspel.erp.controller;

import com.raspel.erp.dto.StokDTO;
import com.raspel.erp.dto.CariHesapDTO;
import com.raspel.erp.service.StokService;
import com.raspel.erp.service.CariHesapService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;

@Tag(name = "Veri Import", description = "CSV ile toplu veri aktarma API")
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class VeriImportController {

    private final StokService stokService;
    private final CariHesapService cariHesapService;

    @PostMapping("/stok")
    @Operation(summary = "CSV ile stok aktar", description = "CSV dosyası ile toplu stok girişi yapar. Kolonlar: ad,stokKodu,barkod,birim,fiyat,miktar,minMiktar")
    public ResponseEntity<Map<String, Object>> stokImport(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Map<String, Object> result = new HashMap<>();
        List<String> hatalar = new ArrayList<>();
        int basarili = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Dosya boş"));
            }
            String[] headers = headerLine.split(";");
            Map<String, Integer> kolonIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                kolonIndex.put(headers[i].trim().toLowerCase(), i);
            }

            String line;
            int satirNo = 1;
            while ((line = br.readLine()) != null) {
                satirNo++;
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(";", -1);
                try {
                    StokDTO dto = StokDTO.builder()
                            .ad(kolonDeger(cols, kolonIndex, "ad", satirNo, hatalar))
                            .stokKodu(kolonDeger(cols, kolonIndex, "stokkodu", satirNo, null))
                            .barkod(kolonDeger(cols, kolonIndex, "barkod", satirNo, null))
                            .birim(kolonDeger(cols, kolonIndex, "birim", satirNo, null))
                            .fiyat(parseBigDecimal(kolonDeger(cols, kolonIndex, "fiyat", satirNo, null)))
                            .miktar(parseBigDecimal(kolonDeger(cols, kolonIndex, "miktar", satirNo, null)))
                            .minMiktar(parseBigDecimal(kolonDeger(cols, kolonIndex, "minmiktar", satirNo, null)))
                            .build();
                    if (dto.getAd() == null) {
                        hatalar.add("Satır " + satirNo + ": ad alanı zorunlu");
                        continue;
                    }
                    stokService.olustur(dto, sirketId);
                    basarili++;
                } catch (Exception e) {
                    hatalar.add("Satır " + satirNo + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dosya okunamadı: " + e.getMessage()));
        }

        result.put("basarili", basarili);
        result.put("hatalar", hatalar);
        result.put("mesaj", basarili + " ürün başarıyla aktarıldı" + (hatalar.isEmpty() ? "." : ", " + hatalar.size() + " hata."));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/cari")
    @Operation(summary = "CSV ile cari hesap aktar", description = "CSV dosyası ile toplu cari hesap girişi yapar. Kolonlar: ad,vergiNo,telefon,eposta,il,ilce,adres")
    public ResponseEntity<Map<String, Object>> cariImport(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Map<String, Object> result = new HashMap<>();
        List<String> hatalar = new ArrayList<>();
        int basarili = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Dosya boş"));
            }
            String[] headers = headerLine.split(";");
            Map<String, Integer> kolonIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                kolonIndex.put(headers[i].trim().toLowerCase(), i);
            }

            String line;
            int satirNo = 1;
            while ((line = br.readLine()) != null) {
                satirNo++;
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(";", -1);
                try {
                    CariHesapDTO dto = CariHesapDTO.builder()
                            .ad(kolonDeger(cols, kolonIndex, "ad", satirNo, hatalar))
                            .vergiNumarasi(kolonDeger(cols, kolonIndex, "vergino", satirNo, null))
                            .telefon(kolonDeger(cols, kolonIndex, "telefon", satirNo, null))
                            .email(kolonDeger(cols, kolonIndex, "eposta", satirNo, null))
                            .il(kolonDeger(cols, kolonIndex, "il", satirNo, null))
                            .ilce(kolonDeger(cols, kolonIndex, "ilce", satirNo, null))
                            .adres(kolonDeger(cols, kolonIndex, "adres", satirNo, null))
                            .build();
                    if (dto.getAd() == null) {
                        hatalar.add("Satır " + satirNo + ": ad alanı zorunlu");
                        continue;
                    }
                    cariHesapService.cariHesapOlustur(dto, sirketId);
                    basarili++;
                } catch (Exception e) {
                    hatalar.add("Satır " + satirNo + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dosya okunamadı: " + e.getMessage()));
        }

        result.put("basarili", basarili);
        result.put("hatalar", hatalar);
        result.put("mesaj", basarili + " cari hesap başarıyla aktarıldı" + (hatalar.isEmpty() ? "." : ", " + hatalar.size() + " hata."));
        return ResponseEntity.ok(result);
    }

    private String kolonDeger(String[] cols, Map<String, Integer> kolonIndex, String key, int satirNo, List<String> hatalar) {
        Integer idx = kolonIndex.get(key);
        if (idx == null || idx >= cols.length) return null;
        String val = cols[idx].trim();
        if (val.isEmpty()) return null;
        return val;
    }

    private BigDecimal parseBigDecimal(String val) {
        if (val == null) return null;
        try { return new BigDecimal(val.replace(",", ".")); } catch (Exception e) { return null; }
    }
}