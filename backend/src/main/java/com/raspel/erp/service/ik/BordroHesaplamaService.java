package com.raspel.erp.service.ik;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ik.BordroHesaplamaDTO;
import com.raspel.erp.entity.ik.BordroAyar;
import com.raspel.erp.entity.ik.MaasBordro;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ik.BordroAyarRepository;
import com.raspel.erp.repository.ik.MaasBordroRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bordro hesaplama motoru. SGK işçi/işsizlik kesintisi, asgari ücret istisnalı gelir
 * vergisi (dilimli), damga vergisi ve işveren maliyeti yıl bazlı {@link BordroAyar}
 * parametreleriyle hesaplanır. Toplu üretim aktif personel için TASLAK bordro oluşturur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BordroHesaplamaService {

    private static final BigDecimal YUZ = new BigDecimal("100");

    private final BordroAyarRepository bordroAyarRepository;
    private final PersonelRepository personelRepository;
    private final MaasBordroRepository maasBordroRepository;
    private final TenantChecker tenantChecker;
    private final ObjectMapper objectMapper;

    @Transactional
    public BordroAyar ayarGetirVeyaOlustur(Long sirketId, Integer yil) {
        int hedefYil = yil != null ? yil : LocalDate.now().getYear();
        return bordroAyarRepository.findBySirketIdAndYil(sirketId, hedefYil)
                .orElseGet(() -> bordroAyarRepository.save(BordroAyar.builder()
                        .sirketId(sirketId).yil(hedefYil)
                        .asgariUcret(BigDecimal.ZERO)
                        .build()));
    }

    @Transactional
    public BordroAyar ayarKaydet(Long sirketId, BordroAyar dto) {
        if (dto.getYil() == null) throw new BusinessException("Yıl zorunludur");
        if (dto.getAsgariUcret() == null || dto.getAsgariUcret().signum() < 0) {
            throw new BusinessException("Asgari ücret negatif olamaz");
        }
        // REDTEAM (Wave 2.3): oranlar sınırsız kaydedilebiliyordu. Negatif veya
        // 100'den büyük bir SGK/damga oranı sessizce TÜM bordroların kesintisini
        // bozuyordu; ayar ekranında da hata görünmüyordu.
        oranKontrol("SGK işçi oranı", dto.getSgkIsciOrani());
        oranKontrol("İşsizlik işçi oranı", dto.getIssizlikIsciOrani());
        oranKontrol("SGK işveren oranı", dto.getSgkIsverenOrani());
        oranKontrol("İşsizlik işveren oranı", dto.getIssizlikIsverenOrani());
        oranKontrol("Damga oranı", dto.getDamgaOrani());
        // Bozuk dilim JSON'u önceden reddet: aksi halde dilimleriCoz() hatayı
        // yutup HERKESE %15 uyguluyor ve kullanıcı nedenini göremiyordu.
        dilimleriDogrula(dto.getGelirVergisiDilimleri());
        BordroAyar ayar = bordroAyarRepository.findBySirketIdAndYil(sirketId, dto.getYil())
                .orElseGet(() -> BordroAyar.builder().sirketId(sirketId).yil(dto.getYil()).build());
        ayar.setAsgariUcret(dto.getAsgariUcret());
        if (dto.getSgkIsciOrani() != null) ayar.setSgkIsciOrani(dto.getSgkIsciOrani());
        if (dto.getIssizlikIsciOrani() != null) ayar.setIssizlikIsciOrani(dto.getIssizlikIsciOrani());
        if (dto.getSgkIsverenOrani() != null) ayar.setSgkIsverenOrani(dto.getSgkIsverenOrani());
        if (dto.getIssizlikIsverenOrani() != null) ayar.setIssizlikIsverenOrani(dto.getIssizlikIsverenOrani());
        if (dto.getDamgaOrani() != null) ayar.setDamgaOrani(dto.getDamgaOrani());
        if (dto.getGelirVergisiDilimleri() != null) ayar.setGelirVergisiDilimleri(dto.getGelirVergisiDilimleri());
        return bordroAyarRepository.save(ayar);
    }

    @Transactional
    public BordroHesaplamaDTO hesapla(BordroHesaplamaDTO istek, Long sirketId) {
        Personel personel = null;
        if (istek.getPersonelId() != null) {
            personel = personelRepository.findById(istek.getPersonelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Personel", istek.getPersonelId()));
            tenantChecker.check(personel.getSirketId(), "Personel");
        }
        Integer yil = istek.getYil() != null ? istek.getYil() : LocalDate.now().getYear();
        BigDecimal brut = istek.getBrutMaas() != null ? istek.getBrutMaas()
                : (personel != null && personel.getMaas() != null ? personel.getMaas() : BigDecimal.ZERO);
        // Kümülatif matrah verilmediyse personelin aynı yıldaki ÖNCEKİ aylarından topla
        // (dilimli verginin doğru hesaplanması için). Önce her ay "ilk ay" gibi hesaplanıyordu.
        BigDecimal kumulatif = istek.getKumulatifMatrah();
        if (kumulatif == null && personel != null && istek.getAy() != null) {
            kumulatif = maasBordroRepository.kumulatifMatrah(sirketId, personel.getId(), yil, istek.getAy());
        }
        BordroAyar ayar = ayarGetirVeyaOlustur(sirketId, yil);
        return hesapla(brut, kumulatif, ayar, personel);
    }

    private BordroHesaplamaDTO hesapla(BigDecimal brutGirdi, BigDecimal kumulatifGirdi,
                                       BordroAyar ayar, Personel personel) {
        BigDecimal brut = nz(brutGirdi);
        BigDecimal kumulatif = nz(kumulatifGirdi);
        BigDecimal sgkOran = nz(ayar.getSgkIsciOrani()).add(nz(ayar.getIssizlikIsciOrani()));
        BigDecimal sgkIsci = yuzde(brut, sgkOran);

        BigDecimal au = nz(ayar.getAsgariUcret());
        BigDecimal auSgk = yuzde(au, sgkOran);
        // Gelir vergisi matrahı: brüt - SGK - asgari ücretin SGK hariç kısmı (istisna).
        BigDecimal matrah = brut.subtract(sgkIsci).subtract(au.subtract(auSgk));
        if (matrah.signum() < 0) matrah = BigDecimal.ZERO;

        // Kümülatif dilimli vergi: bu ayın dilim farkı.
        BigDecimal gvBrut = dilimVergisi(kumulatif.add(matrah), ayar).subtract(dilimVergisi(kumulatif, ayar));
        // Asgari ücrete isabet eden gelir vergisi istisnası.
        BigDecimal auMatrah = au.subtract(auSgk);
        if (auMatrah.signum() < 0) auMatrah = BigDecimal.ZERO;
        BigDecimal auGv = dilimVergisi(kumulatif.add(auMatrah), ayar).subtract(dilimVergisi(kumulatif, ayar));
        BigDecimal gelirVergisi = gvBrut.subtract(auGv);
        if (gelirVergisi.signum() < 0) gelirVergisi = BigDecimal.ZERO;

        // Damga vergisi: asgari ücreti aşan brüt kısım üzerinden (asgari ücret istisnası).
        BigDecimal damgaMatrah = brut.subtract(au);
        if (damgaMatrah.signum() < 0) damgaMatrah = BigDecimal.ZERO;
        BigDecimal damga = yuzde(damgaMatrah, nz(ayar.getDamgaOrani()), 3);

        BigDecimal toplamKesinti = sgkIsci.add(gelirVergisi).add(damga);
        BigDecimal net = brut.subtract(toplamKesinti);
        BigDecimal isverenOran = nz(ayar.getSgkIsverenOrani()).add(nz(ayar.getIssizlikIsverenOrani()));
        BigDecimal isverenMaliyet = brut.add(yuzde(brut, isverenOran));

        return BordroHesaplamaDTO.builder()
                .personelId(personel != null ? personel.getId() : null)
                .personelAdi(personel != null ? (personel.getAd() + " " + personel.getSoyad()) : null)
                .yil(ayar.getYil())
                .brutMaas(brut)
                .kumulatifMatrah(kumulatif)
                .asgariUcret(au)
                .sgkIsciKesintisi(sgkIsci)
                .gelirVergisiMatrahi(matrah)
                .gelirVergisi(gelirVergisi)
                .damgaVergisi(damga)
                .toplamKesinti(toplamKesinti)
                .netMaas(net)
                .isverenMaliyeti(isverenMaliyet)
                .aciklama(au.signum() == 0
                        ? "Asgari ücret tanımlı değil; istisnalar uygulanmadı. Bordro ayarlarını güncelleyin."
                        : null)
                .build();
    }

    /** Kümülatif matrah için dilimli gelir vergisi. Dilim tanımlı değilse %15 sabit uygulanır. */
    private BigDecimal dilimVergisi(BigDecimal matrah, BordroAyar ayar) {
        if (matrah.signum() <= 0) return BigDecimal.ZERO;
        List<BigDecimal[]> dilimler = dilimleriCoz(ayar.getGelirVergisiDilimleri());
        if (dilimler.isEmpty()) {
            return yuzde(matrah, new BigDecimal("15"));
        }
        BigDecimal vergi = BigDecimal.ZERO;
        BigDecimal oncekiLimit = BigDecimal.ZERO;
        for (BigDecimal[] d : dilimler) {
            BigDecimal limit = d[0]; // null = sınırsız
            if (limit != null && matrah.compareTo(oncekiLimit) <= 0) break;
            BigDecimal ust = limit != null ? matrah.min(limit) : matrah;
            BigDecimal dilimTutar = ust.subtract(oncekiLimit);
            if (dilimTutar.signum() > 0) {
                vergi = vergi.add(yuzde(dilimTutar, d[1]));
            }
            if (limit == null) break;
            oncekiLimit = limit;
            if (matrah.compareTo(limit) <= 0) break;
        }
        return vergi;
    }

    private static final BigDecimal YUZDE_MAX = new BigDecimal("100");

    /** Kesinti oranı 0-100 aralığında olmalı; aksi halde bordro kesintisi bozulur. */
    private void oranKontrol(String ad, BigDecimal oran) {
        if (oran == null) return;
        if (oran.signum() < 0 || oran.compareTo(YUZDE_MAX) > 0) {
            throw new BusinessException(ad + " 0 ile 100 arasında olmalıdır. Girilen: " + oran.stripTrailingZeros());
        }
    }

    /**
     * Gelir vergisi dilimlerini kaydetmeden ÖNCE doğrular.
     * <p>Bozuk JSON ya da mantıksız dilimler sessizce {@code %15} sabit
     * uygulamaya düşüyordu; yani bir yazım hatası tüm personelin net maaşını
     * ve vergi sorumluluğunu değiştiriyordu.
     */
    private void dilimleriDogrula(String json) {
        if (json == null || json.isBlank()) return;
        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new BusinessException("Gelir vergisi dilimleri geçerli JSON değil: " + e.getMessage());
        }
        if (!node.isArray() || node.isEmpty()) {
            throw new BusinessException("Gelir vergisi dilimleri boş olamaz");
        }
        BigDecimal oncekiLimit = null;
        boolean oncekiLimitsiz = false;
        int boyut = node.size();
        for (int i = 0; i < boyut; i++) {
            JsonNode d = node.get(i);
            if (!d.hasNonNull("oran")) {
                throw new BusinessException((i + 1) + ". dilimde 'oran' zorunludur");
            }
            oranKontrol((i + 1) + ". dilim oranı", d.get("oran").decimalValue());
            boolean sonDilim = (i == boyut - 1);
            BigDecimal limit = d.hasNonNull("limit") ? d.get("limit").decimalValue() : null;
            if (oncekiLimitsiz) {
                throw new BusinessException("Limit alanı yalnızca son dilimde boş bırakılabilir");
            }
            if (limit == null) {
                if (!sonDilim) {
                    throw new BusinessException("Yalnızca son dilimin limiti boş bırakılabilir");
                }
                oncekiLimitsiz = true;
            } else {
                if (limit.signum() <= 0) {
                    throw new BusinessException((i + 1) + ". dilim limiti pozitif olmalıdır");
                }
                if (oncekiLimit != null && limit.compareTo(oncekiLimit) <= 0) {
                    throw new BusinessException("Dilim limitleri küçükten büyüğe artmalıdır ("
                            + oncekiLimit.stripTrailingZeros() + " -> " + limit.stripTrailingZeros() + ")");
                }
                oncekiLimit = limit;
            }
        }
    }

    /** JSON dilimlerini çözer: [{"limit":158000,"oran":15},{"limit":null,"oran":40}] */
    private List<BigDecimal[]> dilimleriCoz(String json) {
        List<BigDecimal[]> sonuc = new ArrayList<>();
        if (json == null || json.isBlank()) return sonuc;
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) return sonuc;
            for (JsonNode d : node) {
                BigDecimal oran = d.hasNonNull("oran") ? d.get("oran").decimalValue() : null;
                if (oran == null) continue;
                BigDecimal limit = d.hasNonNull("limit") ? d.get("limit").decimalValue() : null;
                sonuc.add(new BigDecimal[]{limit, oran});
            }
        } catch (Exception e) {
            log.warn("Gelir vergisi dilimleri çözülemedi, %15 sabit uygulanacak: {}", e.getMessage());
            return new ArrayList<>();
        }
        return sonuc;
    }

    /**
     * Aktif personel için verilen ayın TASLAK bordrolarını üretir. Aynı personel/ay için
     * bordro varsa atlanır; maaşı tanımsız personel üretilmez.
     */
    @Transactional
    public Map<String, Object> topluUret(Long sirketId, Integer yil, Integer ay) {
        return topluUret(sirketId, yil, ay, null);
    }

    /**
     * Toplu üretim (seçimli). {@code personelIds} null ise tüm aktif personel;
     * verilirse yalnız seçilenler üretilir.
     */
    @Transactional
    public Map<String, Object> topluUret(Long sirketId, Integer yil, Integer ay, List<Long> personelIds) {
        if (yil == null || ay == null || ay < 1 || ay > 12) {
            throw new BusinessException("Yıl ve ay (1-12) zorunludur");
        }
        BordroAyar ayar = ayarGetirVeyaOlustur(sirketId, yil);
        List<Personel> personeller = personelRepository.findBySirketIdAndAktifTrue(sirketId);
        Set<Long> secili = personelIds == null ? null : new HashSet<>(personelIds);
        int uretilen = 0;
        int atlanan = 0;
        for (Personel p : personeller) {
            if (secili != null && !secili.contains(p.getId())) {
                continue; // kullanıcı bu personeli seçmedi
            }
            if (maasBordroRepository.existsBySirketIdAndYilAndAyAndPersonelId(sirketId, yil, ay, p.getId())) {
                atlanan++;
                continue;
            }
            BigDecimal brut = p.getMaas() != null ? p.getMaas() : BigDecimal.ZERO;
            if (brut.signum() <= 0) {
                atlanan++;
                continue;
            }
            // Kümülatif matrah: aynı yıldaki önceki ayların matrah toplamı.
            BigDecimal kumulatif = maasBordroRepository.kumulatifMatrah(sirketId, p.getId(), yil, ay);
            BordroHesaplamaDTO h = hesapla(brut, kumulatif, ayar, p);
            maasBordroRepository.save(MaasBordro.builder()
                    .personel(p).yil(yil).ay(ay)
                    .brutMaas(h.getBrutMaas())
                    .kesintiler(h.getToplamKesinti())
                    .netMaas(h.getNetMaas())
                    .gelirVergisiMatrahi(h.getGelirVergisiMatrahi())
                    .odemeTarihi(LocalDate.of(yil, ay, 1))
                    .sirketId(sirketId)
                    .aciklama("Otomatik hesaplandı")
                    .durum("TASLAK")
                    .build());
            uretilen++;
        }
        Map<String, Object> sonuc = new LinkedHashMap<>();
        sonuc.put("uretilen", uretilen);
        sonuc.put("atlanan", atlanan);
        sonuc.put("toplamPersonel", personeller.size());
        log.info("Toplu bordro üretimi - Şirket: {}, {}/{}, üretilen: {}, atlanan: {}",
                sirketId, ay, yil, uretilen, atlanan);
        return sonuc;
    }

    /**
     * Toplu üretim ÖNİZLEMESİ: hangi personel üretilecek/atlanacak ve tahmini net.
     * durum: UYGUN | MAAS_YOK | ZATEN_VAR.
     */
    @Transactional(readOnly = true)
    public List<com.raspel.erp.dto.ik.BordroOnizlemeDTO> topluOnizleme(Long sirketId, Integer yil, Integer ay) {
        if (yil == null || ay == null || ay < 1 || ay > 12) {
            throw new BusinessException("Yıl ve ay (1-12) zorunludur");
        }
        // Önizleme salt-okunur: ayar yoksa kaydetmeden varsayılan kullan.
        BordroAyar ayar = bordroAyarRepository.findBySirketIdAndYil(sirketId, yil)
                .orElse(BordroAyar.builder().sirketId(sirketId).yil(yil).asgariUcret(java.math.BigDecimal.ZERO).build());
        List<Personel> personeller = personelRepository.findBySirketIdAndAktifTrue(sirketId);
        List<com.raspel.erp.dto.ik.BordroOnizlemeDTO> sonuc = new ArrayList<>();
        for (Personel p : personeller) {
            boolean varMi = maasBordroRepository.existsBySirketIdAndYilAndAyAndPersonelId(sirketId, yil, ay, p.getId());
            BigDecimal brut = p.getMaas() != null ? p.getMaas() : BigDecimal.ZERO;
            String durum = varMi ? "ZATEN_VAR" : (brut.signum() <= 0 ? "MAAS_YOK" : "UYGUN");
            BigDecimal net = null;
            if ("UYGUN".equals(durum)) {
                BigDecimal kumulatif = maasBordroRepository.kumulatifMatrah(sirketId, p.getId(), yil, ay);
                net = hesapla(brut, kumulatif, ayar, p).getNetMaas();
            }
            sonuc.add(com.raspel.erp.dto.ik.BordroOnizlemeDTO.builder()
                    .personelId(p.getId())
                    .personelAdi((p.getAd() != null ? p.getAd() : "") + " " + (p.getSoyad() != null ? p.getSoyad() : ""))
                    .brutMaas(brut).netMaas(net).durum(durum)
                    .build());
        }
        return sonuc;
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private BigDecimal yuzde(BigDecimal tutar, BigDecimal oran) {
        return yuzde(tutar, oran, 2);
    }

    private BigDecimal yuzde(BigDecimal tutar, BigDecimal oran, int scale) {
        return tutar.multiply(oran).divide(YUZ, scale, RoundingMode.HALF_UP);
    }
}
