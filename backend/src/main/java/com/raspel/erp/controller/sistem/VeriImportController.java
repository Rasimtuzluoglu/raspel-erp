package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.envanter.StokDTO;
import com.raspel.erp.dto.finans.CariHesapDTO;
import com.raspel.erp.dto.ticaret.FaturaDTO;
import com.raspel.erp.dto.ticaret.FaturaKalemDTO;
import com.raspel.erp.service.envanter.StokService;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.service.ticaret.FaturaService;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import com.raspel.erp.entity.envanter.Stok;

@Tag(name = "Veri Import", description = "CSV ile toplu veri aktarma API")
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class VeriImportController {

    private final StokService stokService;
    private final CariHesapService cariHesapService;
    private final FaturaService faturaService;
    private final StokRepository stokRepository;
    private final CariHesapRepository cariHesapRepository;

    @PostMapping("/stok")
    @Operation(summary = "CSV ile stok aktar", description = "CSV dosyası ile toplu stok girişi yapar. Kolonlar: ad,stokKodu,barkod,birim,fiyat,miktar,minMiktar")
    @Transactional
    public ResponseEntity<Map<String, Object>> stokImport(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Map<String, Object> result = new HashMap<>();
        List<String> hatalar = new ArrayList<>();
        List<StokDTO> gecerliKayitlar = new ArrayList<>();

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
                    gecerliKayitlar.add(dto);
                } catch (Exception e) {
                    hatalar.add("Satır " + satirNo + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dosya okunamadı: " + e.getMessage()));
        }

        int basarili = 0;
        if (!gecerliKayitlar.isEmpty()) {
            try {
                basarili = stokService.topluOlustur(gecerliKayitlar, sirketId);
            } catch (Exception e) {
                hatalar.add("Toplu kayıt hatası: " + e.getMessage());
            }
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

    @PostMapping("/alis-fatura")
    @Operation(summary = "CSV ile alış faturası aktar", description = "CSV dosyası ile toplu alış faturası girişi yapar. Kolonlar: faturaNo;tarih;cariId;stokKodu;aciklama;adet;birimFiyat;kdvOrani (aynı faturaNo'ya sahip satırlar tek faturada birleştirilir)")
    @Transactional
    public ResponseEntity<Map<String, Object>> alisFaturaImport(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String displayName = (String) request.getAttribute("displayName");
        Map<String, Object> result = new HashMap<>();
        List<String> hatalar = new ArrayList<>();
        Map<String, List<FaturaKalemDTO>> gruplar = new LinkedHashMap<>();
        Map<String, Object> grupMeta = new HashMap<>();

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
                    String faturaNo = kolonDeger(cols, kolonIndex, "faturano", satirNo, hatalar);
                    if (faturaNo == null) { hatalar.add("Satır " + satirNo + ": faturaNo zorunlu"); continue; }
                    String stokKodu = kolonDeger(cols, kolonIndex, "stokkodu", satirNo, hatalar);
                    Stok stok = stokKodu != null ? stokRepository.findBySirketIdAndStokKodu(sirketId, stokKodu).orElse(null) : null;
                    if (stok == null) { hatalar.add("Satır " + satirNo + ": stok bulunamadı -> " + stokKodu); continue; }

                    int adet = Math.max(1, parseInteger(kolonDeger(cols, kolonIndex, "adet", satirNo, null), 1));
                    BigDecimal birimFiyat = parseBigDecimal(kolonDeger(cols, kolonIndex, "birimfiyat", satirNo, null));
                    BigDecimal kdvOrani = parseBigDecimal(kolonDeger(cols, kolonIndex, "kdvorani", satirNo, null));

                    FaturaKalemDTO kalem = FaturaKalemDTO.builder()
                            .aciklama(kolonDeger(cols, kolonIndex, "aciklama", satirNo, null) != null
                                    ? kolonDeger(cols, kolonIndex, "aciklama", satirNo, null) : stok.getAd())
                            .adet(adet)
                            .birimFiyat(birimFiyat != null ? birimFiyat : BigDecimal.ZERO)
                            .kdvOrani(kdvOrani != null ? kdvOrani : new BigDecimal("20"))
                            .stokId(stok.getId())
                            .build();

                    gruplar.computeIfAbsent(faturaNo, k -> new ArrayList<>()).add(kalem);
                    grupMeta.putIfAbsent(faturaNo, new Object[]{kolonDeger(cols, kolonIndex, "tarih", satirNo, null), kolonDeger(cols, kolonIndex, "cariid", satirNo, null)});
                } catch (Exception e) {
                    hatalar.add("Satır " + satirNo + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dosya okunamadı: " + e.getMessage()));
        }

        int basarili = 0;
        for (Map.Entry<String, List<FaturaKalemDTO>> g : gruplar.entrySet()) {
            try {
                Object[] meta = (Object[]) grupMeta.get(g.getKey());
                String tarihStr = meta != null ? (String) meta[0] : null;
                String cariIdStr = meta != null ? (String) meta[1] : null;
                Long cariId = cariIdStr != null && !cariIdStr.isBlank() ? Long.valueOf(cariIdStr) : null;

                FaturaDTO dto = FaturaDTO.builder()
                        .faturaNumarasi(g.getKey())
                        .tur("ALIS")
                        .durum("KESILDI")
                        .tarih(tarihStr != null && !tarihStr.isBlank() ? LocalDate.parse(tarihStr) : LocalDate.now())
                        .cariHesapId(cariId)
                        .kalemler(g.getValue())
                        .build();
                faturaService.faturaOlustur(dto, sirketId, kullaniciId, displayName);
                basarili++;
            } catch (Exception e) {
                hatalar.add("Fatura " + g.getKey() + ": " + e.getMessage());
            }
        }

        result.put("basarili", basarili);
        result.put("hatalar", hatalar);
        result.put("mesaj", basarili + " alış faturası başarıyla aktarıldı" + (hatalar.isEmpty() ? "." : ", " + hatalar.size() + " hata."));
        return ResponseEntity.ok(result);
    }

    private String kolonDeger(String[] cols, Map<String, Integer> kolonIndex, String key, int satirNo, List<String> hatalar) {
        Integer idx = kolonIndex.get(key);
        if (idx == null || idx >= cols.length) return null;
        String val = cols[idx].trim();
        if (val.isEmpty()) return null;
        return val;
    }

    private int parseInteger(String val, int varsayilan) {
        if (val == null) return varsayilan;
        try { return Integer.parseInt(val.trim()); } catch (Exception e) { return varsayilan; }
    }

    private BigDecimal parseBigDecimal(String val) {
        if (val == null) return null;
        try { return new BigDecimal(val.replace(",", ".")); } catch (Exception e) { return null; }
    }
}