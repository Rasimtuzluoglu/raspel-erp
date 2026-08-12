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
import java.util.stream.Collectors;
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
        return bankaHareketiRepository.findByBankaIdOrderByTarihDesc(bankaId)
                .stream().map(this::entityToDTO).collect(Collectors.toList());
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
                : faturaRepository.findAll().stream()
                        .filter(f -> !"ODENDI".equals(f.getOdemeDurumu()))
                        .filter(f -> f.getDurum() != Fatura.FaturaDurum.IPTAL)
                        .collect(Collectors.toList());

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
            if (ad.endsWith(".csv") || ad.endsWith(".txt")) {
                return parseCsv(dosya);
            }
            return parseExcel(dosya);
        } catch (Exception e) {
            throw new BusinessException("Dosya okunamadı: " + e.getMessage());
        }
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
        String faturaNo = null;
        if (h.getEslesenFaturaId() != null) {
            faturaNo = faturaRepository.findById(h.getEslesenFaturaId())
                    .map(Fatura::getFaturaNumarasi).orElse(null);
        }
        return BankaHareketiDTO.builder()
                .id(h.getId()).bankaId(h.getBankaId()).tarih(h.getTarih())
                .aciklama(h.getAciklama()).borc(h.getBorc()).alacak(h.getAlacak())
                .bakiye(h.getBakiye()).eslesenFaturaId(h.getEslesenFaturaId())
                .eslesenFaturaNo(faturaNo).eslestirildi(h.getEslestirildi())
                .sirketId(h.getSirketId()).olusturmaTarihi(h.getOlusturmaTarihi())
                .build();
    }
}