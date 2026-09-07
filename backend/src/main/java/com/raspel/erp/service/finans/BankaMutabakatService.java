package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.BankaHareketiDTO;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.finans.BankaHareketi;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.finans.BankaHareketiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.raspel.erp.entity.finans.Banka;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BankaMutabakatService {

    private final BankaHareketiRepository bankaHareketiRepository;
    private final FaturaRepository faturaRepository;
    private final TenantChecker tenantChecker;

    private static final DateTimeFormatter[] TARIH_FORMATLARI = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };

    public List<BankaHareketiDTO> listele(Long bankaId) {
        List<BankaHareketi> hareketler = bankaHareketiRepository.findByBankaIdOrderByTarihDesc(bankaId);
        // N+1 önlemi: eşleşen fatura numaralarını tek sorguda topla
        Set<Long> faturaIds = hareketler.stream()
                .map(BankaHareketi::getEslesenFaturaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> faturaNoMap = faturaIds.isEmpty() ? Map.of()
                : faturaRepository.findAllById(faturaIds).stream()
                        .collect(Collectors.toMap(Fatura::getId, Fatura::getFaturaNumarasi));

        // Eşleşmemiş hareketler için öneri skorları hesapla
        List<Fatura> acikFaturalar = hareketler.stream().anyMatch(h -> !Boolean.TRUE.equals(h.getEslestirildi()))
                ? faturaRepository.findByTurAndOdemeDurumuNotIn(Fatura.FaturaTur.SATIS, List.of("ODENDI", "IPTAL"))
                : List.of();

        return hareketler.stream()
                .map(h -> entityToDTO(h, faturaNoMap, acikFaturalar))
                .collect(Collectors.toList());
    }

    public int yukle(Long bankaId, MultipartFile dosya, Long sirketId) {
        if (dosya == null || dosya.isEmpty()) throw new BusinessException("Dosya boş olamaz");
        List<String[]> satirlar = parseDosya(dosya);
        if (satirlar.isEmpty()) throw new BusinessException("Dosyadan satır okunamadı");

        List<BankaHareketi> kayitlar = new ArrayList<>();
        for (String[] s : satirlar) {
            try {
                kayitlar.add(satirToHareket(s, bankaId, sirketId));
            } catch (Exception e) {
                log.warn("Satır atlandı ({}): {}", String.join(";", s), e.getMessage());
            }
        }
        List<BankaHareketi> kaydedilen = bankaHareketiRepository.saveAll(kayitlar);
        otomatikEslestir(bankaId, sirketId);
        log.info("Banka mutabakat yüklemesi tamamlandı - Banka: {}, Kayıt: {}", bankaId, kaydedilen.size());
        return kaydedilen.size();
    }

    public List<BankaHareketiDTO> otomatikEslestir(Long bankaId, Long sirketId) {
        List<BankaHareketi> eslesmesiz = bankaHareketiRepository.findByBankaIdAndEslestirildiFalse(bankaId);
        List<Fatura> faturalar = sirketId != null
                ? faturaRepository.findBySirketIdAndDurumNotAndOdemeDurumuNotIn(sirketId, Fatura.FaturaDurum.IPTAL, List.of("ODENDI"))
                : List.of();

        for (BankaHareketi h : eslesmesiz) {
            BigDecimal tutar = h.getBorc().signum() > 0 ? h.getBorc() : h.getAlacak();
            for (Fatura f : faturalar) {
                BigDecimal eslesecek = f.getKalanTutar() != null && f.getKalanTutar().signum() > 0
                        ? f.getKalanTutar() : f.getGenelToplam();
                if (eslesecek != null && eslesecek.compareTo(tutar) == 0
                        && Math.abs(f.getTarih().toEpochDay() - h.getTarih().toEpochDay()) <= 3) {
                    h.setEslestirildi(true);
                    h.setEslesenFaturaId(f.getId());
                    bankaHareketiRepository.save(h);
                    break;
                }
            }
        }
        return listele(bankaId);
    }

    public BankaHareketiDTO eslestir(Long hareketId, Long faturaId) {
        BankaHareketi h = bankaHareketiRepository.findById(hareketId)
                .orElseThrow(() -> new ResourceNotFoundException("Banka hareketi", hareketId));
        tenantChecker.check(h.getSirketId(), "Banka hareketi");
        if (!faturaRepository.existsById(faturaId)) throw new ResourceNotFoundException("Fatura", faturaId);
        h.setEslestirildi(true);
        h.setEslesenFaturaId(faturaId);
        return entityToDTO(bankaHareketiRepository.save(h));
    }

    public BankaHareketiDTO eslestirmeyiKaldir(Long hareketId) {
        BankaHareketi h = bankaHareketiRepository.findById(hareketId)
                .orElseThrow(() -> new ResourceNotFoundException("Banka hareketi", hareketId));
        tenantChecker.check(h.getSirketId(), "Banka hareketi");
        h.setEslestirildi(false);
        h.setEslesenFaturaId(null);
        return entityToDTO(bankaHareketiRepository.save(h));
    }

    public void sil(Long bankaId) {
        List<BankaHareketi> list = bankaHareketiRepository.findByBankaIdOrderByTarihDesc(bankaId);
        bankaHareketiRepository.deleteAll(list);
    }

    // ---------- YARDIMCILAR ----------

    private List<String[]> parseDosya(MultipartFile dosya) {
        String ad = dosya.getOriginalFilename() != null ? dosya.getOriginalFilename().toLowerCase() : "";
        try {
            if (ad.endsWith(".ofx") || ad.endsWith(".qfx")) {
                return parseOfx(dosya);
            }
            if (ad.endsWith(".csv") || ad.endsWith(".txt")) {
                return parseCsv(dosya);
            }
            return parseExcel(dosya);
        } catch (Exception e) {
            throw new BusinessException("Dosya okunamadı: " + e.getMessage());
        }
    }

    /**
     * OFX/QFX (Open Financial Exchange) banka hesap özetini ayrıştırır.
     * STMTTRN bloklarından tarih, açıklama, borç ve alacak bilgisini çıkarır.
     * Dönen satır formatı mevcut CSV ile aynıdır: [tarih, açıklama, borç, alacak, bakiye].
     */
    private List<String[]> parseOfx(MultipartFile dosya) throws Exception {
        String icerik = new String(dosya.getBytes(), StandardCharsets.UTF_8);
        List<String[]> satirlar = new ArrayList<>();

        Matcher m = Pattern.compile("<STMTTRN>(.*?)</STMTTRN>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(icerik);
        DateTimeFormatter ofxTarih = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter hedefTarih = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        while (m.find()) {
            String blok = m.group(1);
            String tarihHam = etiketOku(blok, "DTPOSTED");
            String tutarHam = etiketOku(blok, "TRNAMT");
            if (tarihHam == null || tutarHam == null) continue;

            String tarih;
            try {
                String gun = tarihHam.trim().length() >= 8 ? tarihHam.trim().substring(0, 8) : tarihHam.trim();
                tarih = LocalDate.parse(gun, ofxTarih).format(hedefTarih);
            } catch (Exception e) {
                continue;
            }

            BigDecimal tutar;
            try {
                tutar = new BigDecimal(tutarHam.trim());
            } catch (NumberFormatException e) {
                continue;
            }

            String ad = etiketOku(blok, "NAME");
            String memo = etiketOku(blok, "MEMO");
            String aciklama = (ad != null && !ad.isBlank() ? ad.trim() : "")
                    + (memo != null && !memo.isBlank() ? (ad != null && !ad.isBlank() ? " - " : "") + memo.trim() : "");
            if (aciklama.isBlank()) aciklama = "Banka hareketi";

            String borc = "0";
            String alacak = "0";
            if (tutar.signum() < 0) {
                borc = tutar.abs().toPlainString().replace(".", ",");
            } else {
                alacak = tutar.toPlainString().replace(".", ",");
            }

            satirlar.add(new String[]{tarih, aciklama, borc, alacak, ""});
        }
        return satirlar;
    }

    private String etiketOku(String blok, String etiket) {
        Matcher m = Pattern.compile("<" + etiket + ">(.*?)</" + etiket + ">", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(blok);
        return m.find() ? m.group(1).trim() : null;
    }

    private List<String[]> parseCsv(MultipartFile dosya) throws Exception {
        List<String[]> satirlar = new ArrayList<>();
        BufferedReader okuyucu = new BufferedReader(new InputStreamReader(dosya.getInputStream(), StandardCharsets.UTF_8));
        String satir;
        boolean ilkSatir = true;
        while ((satir = okuyucu.readLine()) != null) {
            if (satir.isBlank()) continue;
            String[] alanlar = satir.split("[;,]");
            // Başlık satırını atla
            if (ilkSatir) {
                ilkSatir = false;
                if (alanlar[0].toLowerCase().contains("tarih") || alanlar[0].toLowerCase().contains("date")) continue;
            }
            if (alanlar.length >= 3) satirlar.add(alanlar);
        }
        return satirlar;
    }

    private List<String[]> parseExcel(MultipartFile dosya) throws Exception {
        List<String[]> satirlar = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(dosya.getInputStream());
        Sheet sayfa = workbook.getSheetAt(0);
        boolean ilkSatir = true;
        for (Row row : sayfa) {
            if (ilkSatir) { ilkSatir = false; continue; }
            String[] alanlar = new String[5];
            for (int i = 0; i < 5; i++) {
                Cell h = row.getCell(i);
                alanlar[i] = h != null ? h.toString().trim() : "";
            }
            if (alanlar[0].isEmpty()) continue;
            satirlar.add(alanlar);
        }
        workbook.close();
        return satirlar;
    }

    private BankaHareketi satirToHareket(String[] s, Long bankaId, Long sirketId) {
        LocalDate tarih = parseTarih(s[0]);
        String aciklama = s.length > 1 ? s[1] : "";
        BigDecimal borc = s.length > 2 ? parseTutar(s[2]) : BigDecimal.ZERO;
        BigDecimal alacak = s.length > 3 ? parseTutar(s[3]) : BigDecimal.ZERO;
        BigDecimal bakiye = s.length > 4 ? parseTutar(s[4]) : null;
        return BankaHareketi.builder()
                .bankaId(bankaId).tarih(tarih).aciklama(aciklama)
                .borc(borc).alacak(alacak).bakiye(bakiye)
                .eslestirildi(false).sirketId(sirketId)
                .build();
    }

    private LocalDate parseTarih(String s) {
        for (DateTimeFormatter f : TARIH_FORMATLARI) {
            try { return LocalDate.parse(s.trim(), f); } catch (DateTimeParseException ignored) {}
        }
        throw new BusinessException("Tarih formatı tanınmadı: " + s);
    }

    private BigDecimal parseTutar(String s) {
        try {
            return new BigDecimal(s.trim().replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private BankaHareketiDTO entityToDTO(BankaHareketi h) {
        return entityToDTO(h, Map.of(), List.of());
    }

    private BankaHareketiDTO entityToDTO(BankaHareketi h, Map<Long, String> faturaNoMap) {
        return entityToDTO(h, faturaNoMap, List.of());
    }

    private BankaHareketiDTO entityToDTO(BankaHareketi h, Map<Long, String> faturaNoMap, List<Fatura> acikFaturalar) {
        String faturaNo = null;
        if (h.getEslesenFaturaId() != null) {
            if (faturaNoMap.containsKey(h.getEslesenFaturaId())) {
                faturaNo = faturaNoMap.get(h.getEslesenFaturaId());
            } else {
                faturaNo = faturaRepository.findById(h.getEslesenFaturaId())
                        .map(Fatura::getFaturaNumarasi).orElse(null);
            }
        }

        Long onerilenId = null;
        String onerilenNo = null;
        Integer skor = null;
        if (!Boolean.TRUE.equals(h.getEslestirildi())) {
            BigDecimal tutar = h.getBorc() != null && h.getBorc().signum() > 0 ? h.getBorc() : h.getAlacak();
            enIyiOneri oneri = enIyiEslesmeOnergesi(tutar, h.getTarih(), acikFaturalar);
            if (oneri != null) {
                onerilenId = oneri.faturaId;
                onerilenNo = oneri.faturaNo;
                skor = oneri.skor;
            }
        }

        return BankaHareketiDTO.builder()
                .id(h.getId()).bankaId(h.getBankaId()).tarih(h.getTarih())
                .aciklama(h.getAciklama()).borc(h.getBorc()).alacak(h.getAlacak())
                .bakiye(h.getBakiye()).eslesenFaturaId(h.getEslesenFaturaId())
                .eslesenFaturaNo(faturaNo).eslestirildi(h.getEslestirildi())
                .onerilenFaturaId(onerilenId).onerilenFaturaNo(onerilenNo).guvenSkoru(skor)
                .sirketId(h.getSirketId()).olusturmaTarihi(h.getOlusturmaTarihi())
                .build();
    }

    private static class enIyiOneri {
        final Long faturaId;
        final String faturaNo;
        final int skor;
        enIyiOneri(Long faturaId, String faturaNo, int skor) {
            this.faturaId = faturaId; this.faturaNo = faturaNo; this.skor = skor;
        }
    }

    /**
     * Bir banka hareketi için en olası fatura eşleşmesini ve güven skorunu (0-100) hesaplar.
     * Skor: tutar eşleşmesi (60) + tarih yakınlığı (40'a kadar). Tutar eşleşmezse öneri döndürülmez.
     */
    private enIyiOneri enIyiEslesmeOnergesi(BigDecimal tutar, LocalDate tarih, List<Fatura> faturalar) {
        if (tutar == null || tutar.signum() == 0 || faturalar.isEmpty()) return null;
        enIyiOneri enIyi = null;
        int enIyiSkor = -1;
        for (Fatura f : faturalar) {
            BigDecimal kalan = f.getKalanTutar() != null && f.getKalanTutar().signum() > 0
                    ? f.getKalanTutar() : f.getGenelToplam();
            if (kalan == null || kalan.compareTo(tutar) != 0) continue;
            long gunFarki = f.getTarih() != null && tarih != null
                    ? Math.abs(f.getTarih().toEpochDay() - tarih.toEpochDay()) : 999;
            int skor;
            if (gunFarki == 0) skor = 100;
            else if (gunFarki <= 3) skor = 80;
            else if (gunFarki <= 7) skor = 60;
            else skor = 50;
            if (skor > enIyiSkor) {
                enIyiSkor = skor;
                enIyi = new enIyiOneri(f.getId(), f.getFaturaNumarasi(), skor);
            }
        }
        return enIyi;
    }
}