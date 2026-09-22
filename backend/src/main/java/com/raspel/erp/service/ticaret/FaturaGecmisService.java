package com.raspel.erp.service.ticaret;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.FaturaGecmisDTO;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.FaturaGecmis;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ticaret.FaturaGecmisRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fatura işlem geçmişini (oluşturma/düzenleme/durum/silme/yazdırma) kaydeder ve okur.
 * Satış ve alış faturaları aynı tabloyu paylaşır.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FaturaGecmisService {

    private static final int OZET_LIMIT = 450;
    private static final Set<String> IZLENEN_ALANLAR = Set.of(
            "faturaNumarasi", "tur", "durum", "cariHesap", "tarih", "vadeTarihi",
            "araToplam", "kdv", "genelToplam", "genelIskontoTutari",
            "odemeDurumu", "odenenTutar", "kalanTutar", "aciklama",
            "depoId", "paraBirimi", "odemeYontemi", "taksitKurum", "taksitTutar",
            "kalemSayisi", "toplamAdet"
    );

    private final FaturaGecmisRepository faturaGecmisRepository;
    private final FaturaRepository faturaRepository;
    private final TenantChecker tenantChecker;
    private final ObjectMapper objectMapper;

    public static final String OLUSTUR = "OLUSTUR";
    public static final String GUNCELLE = "GUNCELLE";
    public static final String DURUM = "DURUM";
    public static final String SIL = "SIL";
    public static final String YAZDIR = "YAZDIR";

    /** Faturanın izlenen alanlarından JSON anlık görüntü üretir. */
    public String snapshot(Fatura fatura) {
        if (fatura == null) return null;
        Map<String, String> m = new LinkedHashMap<>();
        m.put("faturaNumarasi", metin(fatura.getFaturaNumarasi()));
        m.put("tur", metin(fatura.getTur()));
        m.put("durum", metin(fatura.getDurum()));
        m.put("cariHesap", fatura.getCariHesap() != null ? metin(fatura.getCariHesap().getAd()) : null);
        m.put("tarih", metin(fatura.getTarih()));
        m.put("vadeTarihi", metin(fatura.getVadeTarihi()));
        m.put("araToplam", metin(fatura.getAraToplam()));
        m.put("kdv", metin(fatura.getKdv()));
        m.put("genelToplam", metin(fatura.getGenelToplam()));
        m.put("genelIskontoTutari", metin(fatura.getGenelIskontoTutari()));
        m.put("odemeDurumu", metin(fatura.getOdemeDurumu()));
        m.put("odenenTutar", metin(fatura.getOdenenTutar()));
        m.put("kalanTutar", metin(fatura.getKalanTutar()));
        m.put("aciklama", metin(fatura.getAciklama()));
        m.put("depoId", metin(fatura.getDepoId()));
        m.put("paraBirimi", metin(fatura.getParaBirimi()));
        m.put("odemeYontemi", metin(fatura.getOdemeYontemi()));
        m.put("taksitKurum", metin(fatura.getTaksitKurum()));
        m.put("taksitTutar", metin(fatura.getTaksitTutar()));
        if (fatura.getKalemler() != null) {
            m.put("kalemSayisi", String.valueOf(fatura.getKalemler().size()));
            java.math.BigDecimal toplamAdet = fatura.getKalemler().stream()
                    .map(k -> k.getAdet() != null ? k.getAdet() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            m.put("toplamAdet", toplamAdet.stripTrailingZeros().toPlainString());
        }
        try {
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            log.warn("Fatura snapshot serileştirilemedi: {}", e.getMessage());
            return null;
        }
    }

    /** İki JSON anlık görüntü arasındaki değişen alanları "alan: eski → yeni" olarak özetler. */
    public String diffOzet(String oncekiJson, String yeniJson) {
        if (oncekiJson == null || yeniJson == null) return null;
        try {
            Map<?, ?> onceki = objectMapper.readValue(oncekiJson, Map.class);
            Map<?, ?> yeni = objectMapper.readValue(yeniJson, Map.class);
            StringBuilder sb = new StringBuilder();
            for (String alan : IZLENEN_ALANLAR) {
                Object o = onceki.get(alan);
                Object y = yeni.get(alan);
                if (o == null && y == null) continue;
                if (o != null && o.equals(y)) continue;
                if (sb.length() > 0) sb.append("; ");
                sb.append(alan).append(": ").append(o == null ? "-" : o)
                        .append(" → ").append(y == null ? "-" : y);
            }
            return sb.length() == 0 ? null : kisalt(sb.toString());
        } catch (Exception e) {
            log.warn("Fatura geçmiş fark özeti üretilemedi: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Geçmiş kaydı oluşturur. Kullanıcı/IP request bağlamından alınır; yoksa faturanın
     * oluşturan kullanıcı bilgisi kullanılır. Hata iş akışını bloklamaz.
     */
    public void kaydet(Fatura fatura, String olay, String aciklama, String oncekiDeger, String yeniDeger) {
        kaydet(fatura, olay, aciklama, oncekiDeger, yeniDeger, null, null);
    }

    public void kaydet(Fatura fatura, String olay, String aciklama, String oncekiDeger, String yeniDeger,
                       String yazdirmaFormat, String yaziciAdi) {
        if (fatura == null || fatura.getId() == null) return;
        try {
            FaturaGecmis.FaturaGecmisBuilder builder = FaturaGecmis.builder()
                    .faturaId(fatura.getId())
                    .sirketId(fatura.getSirketId())
                    .olay(olay)
                    .aciklama(kisalt(aciklama))
                    .oncekiDeger(oncekiDeger)
                    .yeniDeger(yeniDeger)
                    .yazdirmaFormat(yazdirmaFormat)
                    .yaziciAdi(yaziciAdi);
            baglamDoldur(builder, fatura);
            if (YAZDIR.equals(olay)) {
                builder.kopyaNo((int) (faturaGecmisRepository.countByFaturaIdAndOlay(fatura.getId(), YAZDIR) + 1));
            }
            faturaGecmisRepository.save(builder.build());
        } catch (Exception e) {
            log.warn("Fatura geçmiş kaydı oluşturulamadı (fatura {}, olay {}): {}", fatura.getId(), olay, e.getMessage());
        }
    }

    /** Yazdırma olayını kaydeder ve oluşan kaydı döner. */
    public FaturaGecmisDTO yazdirmaKaydet(Long faturaId, String format, String yaziciAdi) {
        Fatura fatura = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Fatura", faturaId));
        tenantChecker.check(fatura.getSirketId(), "Fatura");
        String f = format == null || format.isBlank() ? "A4" : format.trim().toUpperCase();
        String aciklama = "Yazdırıldı (" + f + ")" + (yaziciAdi != null && !yaziciAdi.isBlank() ? " - " + yaziciAdi.trim() : "");
        kaydet(fatura, YAZDIR, aciklama, null, null, f, yaziciAdi != null ? yaziciAdi.trim() : null);
        List<FaturaGecmis> liste = faturaGecmisRepository.findByFaturaIdOrderByTarihDescIdDesc(faturaId);
        return liste.isEmpty() ? null : dto(liste.get(0));
    }

    @Transactional(readOnly = true)
    public List<FaturaGecmisDTO> gecmis(Long faturaId) {
        Fatura fatura = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Fatura", faturaId));
        tenantChecker.check(fatura.getSirketId(), "Fatura");
        return faturaGecmisRepository.findByFaturaIdOrderByTarihDescIdDesc(faturaId).stream()
                .map(this::dto).collect(Collectors.toList());
    }

    /** Rapor: filtrelenmiş işlem/yazdırma geçmişi (fatura bilgileriyle). */
    @Transactional(readOnly = true)
    public List<com.raspel.erp.dto.ticaret.FaturaGecmisRaporDTO> rapor(Long sirketId, LocalDate baslangic,
                                                                       LocalDate bitis, String olay,
                                                                       Long kullaniciId, String tur, String q) {
        if (sirketId == null) return List.of();
        java.time.LocalDateTime bas = baslangic != null ? baslangic.atStartOfDay() : null;
        java.time.LocalDateTime bit = bitis != null ? bitis.atTime(java.time.LocalTime.MAX) : null;
        // Dinamik Specification: null parametreler sorguya eklenmez (Postgres tip hatasi onlenir).
        var spec = org.springframework.data.jpa.domain.Specification.<FaturaGecmis>where(
                (root, query, cb) -> cb.equal(root.get("sirketId"), sirketId));
        if (olay != null && !olay.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("olay"), olay));
        }
        if (kullaniciId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("kullaniciId"), kullaniciId));
        }
        if (bas != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("tarih"), bas));
        }
        if (bit != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("tarih"), bit));
        }
        var sirala = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Order.desc("tarih"),
                org.springframework.data.domain.Sort.Order.desc("id"));
        List<FaturaGecmis> kayitlar = faturaGecmisRepository.findAll(spec, sirala);
        if (kayitlar.isEmpty()) return List.of();
        List<Long> faturaIds = kayitlar.stream().map(FaturaGecmis::getFaturaId).distinct().toList();
        Map<Long, Fatura> faturaMap = faturaRepository.findAllById(faturaIds).stream()
                .collect(Collectors.toMap(Fatura::getId, f -> f));
        String turFiltre = tur != null && !tur.isBlank() ? tur.trim().toUpperCase() : null;
        String arama = q != null && !q.isBlank() ? q.trim().toLowerCase() : null;
        List<com.raspel.erp.dto.ticaret.FaturaGecmisRaporDTO> sonuc = new java.util.ArrayList<>();
        for (FaturaGecmis g : kayitlar) {
            Fatura f = faturaMap.get(g.getFaturaId());
            String faturaTur = f != null && f.getTur() != null ? f.getTur().name() : null;
            if (turFiltre != null && !turFiltre.equals(faturaTur)) continue;
            com.raspel.erp.dto.ticaret.FaturaGecmisRaporDTO dto = com.raspel.erp.dto.ticaret.FaturaGecmisRaporDTO.builder()
                    .id(g.getId()).faturaId(g.getFaturaId())
                    .faturaNumarasi(f != null ? f.getFaturaNumarasi() : null)
                    .faturaTur(faturaTur)
                    .faturaDurum(f != null && f.getDurum() != null ? f.getDurum().name() : null)
                    .cariHesapAd(f != null && f.getCariHesap() != null ? f.getCariHesap().getAd() : null)
                    .olay(g.getOlay()).aciklama(g.getAciklama())
                    .kullaniciAdi(g.getKullaniciAdi()).ipAdresi(g.getIpAdresi())
                    .yazdirmaFormat(g.getYazdirmaFormat()).yaziciAdi(g.getYaziciAdi()).kopyaNo(g.getKopyaNo())
                    .tarih(g.getTarih()).build();
            if (arama != null) {
                String havuz = ((dto.getFaturaNumarasi() != null ? dto.getFaturaNumarasi() : "") + " "
                        + (dto.getCariHesapAd() != null ? dto.getCariHesapAd() : "") + " "
                        + (dto.getKullaniciAdi() != null ? dto.getKullaniciAdi() : "") + " "
                        + (dto.getAciklama() != null ? dto.getAciklama() : "")).toLowerCase();
                if (!havuz.contains(arama)) continue;
            }
            sonuc.add(dto);
        }
        return sonuc;
    }

    /** Liste için: her fatura id'sine karşılık yazdırma sayısı + son yazdırma bilgisi. */
    @Transactional(readOnly = true)
    public Map<Long, com.raspel.erp.dto.ticaret.FaturaYazdirmaOzetDTO> yazdirmaOzetleri(List<Long> faturaIds) {
        if (faturaIds == null || faturaIds.isEmpty()) return Map.of();
        Map<Long, com.raspel.erp.dto.ticaret.FaturaYazdirmaOzetDTO> sonuc = new LinkedHashMap<>();
        for (FaturaGecmis g : faturaGecmisRepository.findByFaturaIdInAndOlayOrderByTarihDescIdDesc(faturaIds, YAZDIR)) {
            com.raspel.erp.dto.ticaret.FaturaYazdirmaOzetDTO mevcut = sonuc.get(g.getFaturaId());
            if (mevcut == null) {
                sonuc.put(g.getFaturaId(), com.raspel.erp.dto.ticaret.FaturaYazdirmaOzetDTO.builder()
                        .faturaId(g.getFaturaId()).adet(1).sonTarih(g.getTarih())
                        .sonFormat(g.getYazdirmaFormat()).sonYazici(g.getYaziciAdi())
                        .build());
            } else {
                mevcut.setAdet(mevcut.getAdet() + 1);
            }
        }
        return sonuc;
    }

    private void baglamDoldur(FaturaGecmis.FaturaGecmisBuilder builder, Fatura fatura) {
        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            builder.kullaniciId((Long) req.getAttribute("kullaniciId"));
            Object dn = req.getAttribute("displayName");
            builder.kullaniciAdi(dn instanceof String && !((String) dn).isBlank()
                    ? (String) dn : fatura.getOlusturanKullaniciAdi());
            builder.ipAdresi(req.getRemoteAddr());
        } catch (Exception ignored) {
            builder.kullaniciId(fatura.getOlusturanKullaniciId());
            builder.kullaniciAdi(fatura.getOlusturanKullaniciAdi());
        }
    }

    private FaturaGecmisDTO dto(FaturaGecmis g) {
        return FaturaGecmisDTO.builder()
                .id(g.getId()).faturaId(g.getFaturaId()).olay(g.getOlay()).aciklama(g.getAciklama())
                .oncekiDeger(g.getOncekiDeger()).yeniDeger(g.getYeniDeger())
                .kullaniciId(g.getKullaniciId()).kullaniciAdi(g.getKullaniciAdi()).ipAdresi(g.getIpAdresi())
                .yazdirmaFormat(g.getYazdirmaFormat()).yaziciAdi(g.getYaziciAdi()).kopyaNo(g.getKopyaNo())
                .tarih(g.getTarih())
                .build();
    }

    private String metin(Object deger) {
        if (deger == null) return null;
        if (deger instanceof LocalDate ld) return ld.toString();
        return String.valueOf(deger);
    }

    private String kisalt(String s) {
        if (s == null) return null;
        return s.length() <= OZET_LIMIT ? s : s.substring(0, OZET_LIMIT - 1) + "…";
    }
}
