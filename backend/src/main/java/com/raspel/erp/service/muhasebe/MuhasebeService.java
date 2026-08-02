package com.raspel.erp.service.muhasebe;

import com.raspel.erp.dto.muhasebe.*;
import com.raspel.erp.entity.muhasebe.HesapPlani;
import com.raspel.erp.entity.muhasebe.MuhasebeFisKalem;
import com.raspel.erp.entity.muhasebe.MuhasebeFisi;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.DuplicateResourceException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.muhasebe.HesapPlaniRepository;
import com.raspel.erp.repository.muhasebe.MuhasebeFisKalemRepository;
import com.raspel.erp.repository.muhasebe.MuhasebeFisiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MuhasebeService {

    private final HesapPlaniRepository hesapPlaniRepository;
    private final MuhasebeFisiRepository muhasebeFisiRepository;
    private final MuhasebeFisKalemRepository muhasebeFisKalemRepository;

    // ---------- HESAP PLANI ----------

    @Transactional(readOnly = true)
    public List<HesapPlaniDTO> hesapPlaniniGetir(Long sirketId) {
        List<HesapPlani> list = hesapPlaniRepository.findBySirketIdOrderByKodAsc(sirketId);
        if (list.isEmpty()) {
            varsayilanHesapPlaniniOlustur(sirketId);
            list = hesapPlaniRepository.findBySirketIdOrderByKodAsc(sirketId);
        }
        return list.stream().map(this::hesapEntityToDTO).collect(Collectors.toList());
    }

    public HesapPlaniDTO hesapOlustur(HesapPlaniDTO dto) {
        hesapPlaniRepository.findBySirketIdAndKod(dto.getSirketId(), dto.getKod()).ifPresent(h -> {
            throw new DuplicateResourceException("Bu hesap kodu zaten tanımlı: " + dto.getKod());
        });
        HesapPlani hesap = HesapPlani.builder()
                .kod(dto.getKod())
                .ad(dto.getAd())
                .tip(dto.getTip())
                .grup(dto.getGrup())
                .ustId(dto.getUstId())
                .sirketId(dto.getSirketId())
                .aktif(dto.getAktif() != null ? dto.getAktif() : true)
                .build();
        return hesapEntityToDTO(hesapPlaniRepository.save(hesap));
    }

    public HesapPlaniDTO hesapGuncelle(Long id, HesapPlaniDTO dto) {
        HesapPlani hesap = hesapPlaniRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hesap Planı", id));
        if (dto.getAd() != null) hesap.setAd(dto.getAd());
        if (dto.getTip() != null) hesap.setTip(dto.getTip());
        if (dto.getGrup() != null) hesap.setGrup(dto.getGrup());
        if (dto.getUstId() != null) hesap.setUstId(dto.getUstId());
        if (dto.getAktif() != null) hesap.setAktif(dto.getAktif());
        return hesapEntityToDTO(hesapPlaniRepository.save(hesap));
    }

    public void hesapSil(Long id) {
        if (!hesapPlaniRepository.existsById(id)) throw new ResourceNotFoundException("Hesap Planı", id);
        hesapPlaniRepository.deleteById(id);
    }

    // ---------- YEVMIYE (MUHASEBE FİŞİ) ----------

    @Transactional(readOnly = true)
    public List<MuhasebeFisiDTO> fisleriGetir(Long sirketId, LocalDate baslangic, LocalDate bitis) {
        LocalDate bas = baslangic != null ? baslangic : LocalDate.now().minusMonths(12);
        LocalDate bit = bitis != null ? bitis : LocalDate.now();
        return muhasebeFisiRepository.findBySirketIdAndTarihBetweenOrderByTarihAsc(sirketId, bas, bit)
                .stream().map(f -> fisEntityToDTO(f, true)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MuhasebeFisiDTO fisGetir(Long id) {
        MuhasebeFisi fis = muhasebeFisiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Muhasebe Fişi", id));
        return fisEntityToDTO(fis, true);
    }

    public MuhasebeFisiDTO fisOlustur(MuhasebeFisiDTO dto) {
        if (dto.getKalemler() == null || dto.getKalemler().isEmpty()) {
            throw new BusinessException("Fişe en az bir kalem eklenmelidir");
        }
        BigDecimal toplamBorc = BigDecimal.ZERO;
        BigDecimal toplamAlacak = BigDecimal.ZERO;
        for (MuhasebeFisKalemDTO k : dto.getKalemler()) {
            BigDecimal borc = k.getBorc() != null ? k.getBorc() : BigDecimal.ZERO;
            BigDecimal alacak = k.getAlacak() != null ? k.getAlacak() : BigDecimal.ZERO;
            if (borc.compareTo(BigDecimal.ZERO) < 0 || alacak.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Borç ve alacak tutarları negatif olamaz");
            }
            hesapKodunuDogrula(dto.getSirketId(), k.getHesapKodu());
            toplamBorc = toplamBorc.add(borc);
            toplamAlacak = toplamAlacak.add(alacak);
        }
        if (toplamBorc.compareTo(toplamAlacak) != 0) {
            throw new BusinessException("Fiş denk değil: toplam borç (" + toplamBorc + ") ile toplam alacak (" + toplamAlacak + ") eşit olmalıdır");
        }

        MuhasebeFisi fis = MuhasebeFisi.builder()
                .fisNo(siradakiFisNo(dto.getSirketId()))
                .tarih(dto.getTarih())
                .aciklama(dto.getAciklama())
                .durum("KAYITLI")
                .sirketId(dto.getSirketId())
                .kullaniciId(dto.getKullaniciId())
                .build();
        MuhasebeFisi saved = muhasebeFisiRepository.save(fis);

        List<MuhasebeFisKalem> kalemler = new ArrayList<>();
        for (MuhasebeFisKalemDTO k : dto.getKalemler()) {
            HesapPlani hesap = hesapPlaniRepository.findBySirketIdAndKod(dto.getSirketId(), k.getHesapKodu())
                    .orElse(null);
            kalemler.add(MuhasebeFisKalem.builder()
                    .fisId(saved.getId())
                    .hesapKodu(k.getHesapKodu())
                    .hesapAdi(k.getHesapAdi() != null ? k.getHesapAdi() : (hesap != null ? hesap.getAd() : null))
                    .borc(k.getBorc() != null ? k.getBorc() : BigDecimal.ZERO)
                    .alacak(k.getAlacak() != null ? k.getAlacak() : BigDecimal.ZERO)
                    .aciklama(k.getAciklama())
                    .build());
        }
        muhasebeFisKalemRepository.saveAll(kalemler);
        log.info("Muhasebe fişi oluşturuldu - Fiş No: {}, Şirket: {}", saved.getFisNo(), dto.getSirketId());
        return fisEntityToDTO(saved, true);
    }

    public void fisIptalEt(Long id) {
        MuhasebeFisi fis = muhasebeFisiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Muhasebe Fişi", id));
        if ("ONAYLANDI".equals(fis.getDurum())) {
            throw new BusinessException("Onaylanmış fiş iptal edilemez, düzeltme fişi açınız");
        }
        fis.setDurum("IPTAL");
        muhasebeFisiRepository.save(fis);
    }

    public void fisSil(Long id) {
        if (!muhasebeFisiRepository.existsById(id)) throw new ResourceNotFoundException("Muhasebe Fişi", id);
        muhasebeFisKalemRepository.deleteByFisId(id);
        muhasebeFisiRepository.deleteById(id);
    }

    // ---------- MİZAN ----------

    @Transactional(readOnly = true)
    public List<MizanSatiriDTO> mizanGetir(Long sirketId, LocalDate baslangic, LocalDate bitis) {
        LocalDate bas = baslangic != null ? baslangic : LocalDate.now().minusYears(1);
        LocalDate bit = bitis != null ? bitis : LocalDate.now();
        List<MuhasebeFisKalem> kalemler = muhasebeFisKalemRepository.findBySirketId(sirketId);
        Map<String, MizanSatiriDTO> harita = new TreeMap<>();

        for (MuhasebeFisKalem k : kalemler) {
            MizanSatiriDTO satir = harita.computeIfAbsent(k.getHesapKodu(), kod -> MizanSatiriDTO.builder()
                    .hesapKodu(kod)
                    .hesapAdi(k.getHesapAdi())
                    .borc(BigDecimal.ZERO)
                    .alacak(BigDecimal.ZERO)
                    .borcBakiye(BigDecimal.ZERO)
                    .alacakBakiye(BigDecimal.ZERO)
                    .build());
            satir.setBorc(satir.getBorc().add(k.getBorc()));
            satir.setAlacak(satir.getAlacak().add(k.getAlacak()));
        }

        List<MizanSatiriDTO> sonuc = new ArrayList<>();
        for (MizanSatiriDTO s : harita.values()) {
            BigDecimal bakiye = s.getBorc().subtract(s.getAlacak());
            if (bakiye.compareTo(BigDecimal.ZERO) >= 0) {
                s.setBorcBakiye(bakiye);
                s.setAlacakBakiye(BigDecimal.ZERO);
            } else {
                s.setAlacakBakiye(bakiye.abs());
                s.setBorcBakiye(BigDecimal.ZERO);
            }
            sonuc.add(s);
        }
        return sonuc;
    }

    // ---------- DEFTER-İ KEBİR ----------

    @Transactional(readOnly = true)
    public List<DefteriKebirSatiriDTO> defteriKebirGetir(Long sirketId, String hesapKodu, LocalDate baslangic, LocalDate bitis) {
        LocalDate bas = baslangic != null ? baslangic : LocalDate.now().minusYears(1);
        LocalDate bit = bitis != null ? bitis : LocalDate.now();
        List<MuhasebeFisKalem> kalemler = muhasebeFisKalemRepository.findBySirketId(sirketId);
        Map<Long, MuhasebeFisi> fisMap = new HashMap<>();
        muhasebeFisiRepository.findAll().forEach(f -> fisMap.put(f.getId(), f));

        List<DefteriKebirSatiriDTO> sonuc = new ArrayList<>();
        BigDecimal bakiye = BigDecimal.ZERO;
        for (MuhasebeFisKalem k : kalemler.stream()
                .filter(k -> hesapKodu == null || k.getHesapKodu().equals(hesapKodu))
                .sorted(Comparator.comparing(k -> ((MuhasebeFisKalem) k).getFisId()))
                .collect(Collectors.toList())) {
            MuhasebeFisi fis = fisMap.get(k.getFisId());
            if (fis == null || "IPTAL".equals(fis.getDurum())) continue;
            if (fis.getTarih().isBefore(bas) || fis.getTarih().isAfter(bit)) continue;
            bakiye = bakiye.add(k.getBorc()).subtract(k.getAlacak());
            sonuc.add(DefteriKebirSatiriDTO.builder()
                    .tarih(fis.getTarih())
                    .fisNo(fis.getFisNo())
                    .aciklama(fis.getAciklama())
                    .borc(k.getBorc())
                    .alacak(k.getAlacak())
                    .bakiye(bakiye.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        return sonuc;
    }

    // ---------- YARDIMCILAR ----------

    private void hesapKodunuDogrula(Long sirketId, String kod) {
        if (kod == null || kod.isBlank()) throw new BusinessException("Hesap kodu boş olamaz");
        hesapPlaniRepository.findBySirketIdAndKod(sirketId, kod).orElseThrow(() ->
                new BusinessException("Hesap planında '" + kod + "' kodu bulunamadı"));
    }

    private String siradakiFisNo(Long sirketId) {
        String sonFisNo = muhasebeFisiRepository.findTopBySirketIdOrderByFisNoDesc(sirketId)
                .map(MuhasebeFisi::getFisNo).orElse(null);
        int sayi = 1;
        if (sonFisNo != null && sonFisNo.length() > 9) {
            try {
                sayi = Integer.parseInt(sonFisNo.substring(sonFisNo.length() - 6)) + 1;
            } catch (NumberFormatException ignored) {}
        }
        return "MUH-" + LocalDate.now().getYear() + "-" + String.format("%06d", sayi);
    }

    private void varsayilanHesapPlaniniOlustur(Long sirketId) {
        Map<String, String[]> varsayilanlar = new LinkedHashMap<>();
        varsayilanlar.put("100", new String[]{"Kasa", "AKTIF"});
        varsayilanlar.put("101", new String[]{"Alınan Çekler", "AKTIF"});
        varsayilanlar.put("102", new String[]{"Bankalar", "AKTIF"});
        varsayilanlar.put("120", new String[]{"Alıcılar", "AKTIF"});
        varsayilanlar.put("153", new String[]{"Ticari Mallar", "AKTIF"});
        varsayilanlar.put("191", new String[]{"İndirilecek KDV", "AKTIF"});
        varsayilanlar.put("320", new String[]{"Satıcılar", "PASIF"});
        varsayilanlar.put("360", new String[]{"Ödenecek Vergi ve Fonlar", "PASIF"});
        varsayilanlar.put("391", new String[]{"Hesaplanan KDV", "PASIF"});
        varsayilanlar.put("500", new String[]{"Sermaye", "PASIF"});
        varsayilanlar.put("600", new String[]{"Yurt İçi Satışlar", "GELIR"});
        varsayilanlar.put("601", new String[]{"Yurt Dışı Satışlar", "GELIR"});
        varsayilanlar.put("620", new String[]{"Satılan Ticari Mallar Maliyeti", "GIDER"});
        varsayilanlar.put("770", new String[]{"Genel Yönetim Giderleri", "GIDER"});
        varsayilanlar.put("780", new String[]{"Finansman Giderleri", "GIDER"});

        List<HesapPlani> liste = new ArrayList<>();
        varsayilanlar.forEach((kod, meta) -> {
            HesapPlani hesap = HesapPlani.builder()
                    .kod(kod).ad(meta[0]).tip(meta[1])
                    .sirketId(sirketId).aktif(true)
                    .build();
            liste.add(hesap);
        });
        hesapPlaniRepository.saveAll(liste);
        log.info("Varsayılan hesap planı oluşturuldu - Şirket: {}", sirketId);
    }

    private HesapPlaniDTO hesapEntityToDTO(HesapPlani h) {
        return HesapPlaniDTO.builder()
                .id(h.getId()).kod(h.getKod()).ad(h.getAd()).tip(h.getTip())
                .grup(h.getGrup()).ustId(h.getUstId()).sirketId(h.getSirketId()).aktif(h.getAktif())
                .build();
    }

    private MuhasebeFisiDTO fisEntityToDTO(MuhasebeFisi f, boolean kalemlerDahil) {
        List<MuhasebeFisKalem> kalemler = muhasebeFisKalemRepository.findByFisIdOrderByIdAsc(f.getId());
        BigDecimal borc = kalemler.stream().map(MuhasebeFisKalem::getBorc).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal alacak = kalemler.stream().map(MuhasebeFisKalem::getAlacak).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return MuhasebeFisiDTO.builder()
                .id(f.getId()).fisNo(f.getFisNo()).tarih(f.getTarih())
                .aciklama(f.getAciklama()).durum(f.getDurum())
                .sirketId(f.getSirketId()).kullaniciId(f.getKullaniciId())
                .olusturmaTarihi(f.getOlusturmaTarihi())
                .toplamBorc(borc).toplamAlacak(alacak)
                .kalemler(kalemlerDahil ? kalemler.stream().map(this::kalemEntityToDTO).collect(Collectors.toList()) : null)
                .build();
    }

    private MuhasebeFisKalemDTO kalemEntityToDTO(MuhasebeFisKalem k) {
        return MuhasebeFisKalemDTO.builder()
                .id(k.getId()).hesapKodu(k.getHesapKodu()).hesapAdi(k.getHesapAdi())
                .borc(k.getBorc()).alacak(k.getAlacak()).aciklama(k.getAciklama())
                .build();
    }
}
