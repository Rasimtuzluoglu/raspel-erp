package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.FaturaDTO;
import com.raspel.erp.dto.ticaret.FaturaKalemDTO;
import com.raspel.erp.dto.ticaret.SatinalmaSiparisDTO;
import com.raspel.erp.dto.ticaret.SatinalmaSiparisKalemDTO;
import com.raspel.erp.dto.envanter.StokHareketDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.SatinalmaSiparis;
import com.raspel.erp.entity.ticaret.SatinalmaSiparisKalem;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.SatinalmaSiparisKalemRepository;
import com.raspel.erp.repository.ticaret.SatinalmaSiparisRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.raspel.erp.repository.ticaret.SiparisRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class SatinalmaSiparisService {

    private final SatinalmaSiparisRepository siparisRepository;
    private final SatinalmaSiparisKalemRepository kalemRepository;
    private final CariHesapRepository cariHesapRepository;
    private final StokRepository stokRepository;
    private final TenantChecker tenantChecker;
    private final FaturaService faturaService;
    private final com.raspel.erp.service.envanter.StokService stokService;
    private final com.raspel.erp.service.sistem.DonemService donemService;
    private final com.raspel.erp.repository.ticaret.SatinalmaTalepRepository talepRepository;

    @org.springframework.beans.factory.annotation.Value("${app.kdv.varsayilan-oran:20}")
    private BigDecimal varsayilanKdvOrani;

    @Transactional(readOnly = true)
    public Page<SatinalmaSiparisDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        Page<SatinalmaSiparis> sayfa = siparisRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable);
        List<SatinalmaSiparis> siparisler = sayfa.getContent();
        if (siparisler.isEmpty()) return sayfa.map(this::entityToDTO);

        // N+1 onlemi: kalem, stok ve cari verilerini toplu sorgularla getir.
        List<Long> siparisIdler = siparisler.stream().map(SatinalmaSiparis::getId).collect(Collectors.toList());
        Map<Long, List<SatinalmaSiparisKalem>> kalemMap = kalemRepository.findBySiparisIdIn(siparisIdler).stream()
                .collect(Collectors.groupingBy(SatinalmaSiparisKalem::getSiparisId));

        Map<Long, Stok> stokMap = stokMapOlustur(kalemMap.values().stream()
                .flatMap(List::stream).map(SatinalmaSiparisKalem::getStokId).collect(Collectors.toSet()));
        Map<Long, CariHesap> cariMap = cariMapOlustur(siparisler.stream()
                .map(SatinalmaSiparis::getCariHesapId).collect(Collectors.toSet()));

        return sayfa.map(s -> entityToDTO(s,
                kalemMap.getOrDefault(s.getId(), List.of()), stokMap, cariMap));
    }

    @Transactional(readOnly = true)
    public SatinalmaSiparisDTO getir(Long id) {
        SatinalmaSiparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        return entityToDTO(s);
    }

    public SatinalmaSiparisDTO olustur(SatinalmaSiparisDTO dto) {
        return olustur(dto, null);
    }

    public SatinalmaSiparisDTO olustur(SatinalmaSiparisDTO dto, Long sirketId) {
        // Tenant baglami request'ten gelir; DTO'da yoksa/null ise buradan set edilir.
        if (sirketId != null) {
            dto.setSirketId(sirketId);
        }
        donemService.kilitKontrol(sirketId, dto.getTarih() != null ? dto.getTarih() : LocalDate.now(), "satınalma siparişi oluşturma");

        // Talepten donusum: talep dogrulanir ve ayni islemde SIPARISE_DONUSTU yapilir.
        // Aksi halde ayni talep icin sinirsiz siparis uretilebiliyordu.
        com.raspel.erp.entity.ticaret.SatinalmaTalep talep = null;
        if (dto.getTalepId() != null) {
            talep = talepRepository.findById(dto.getTalepId())
                    .orElseThrow(() -> new ResourceNotFoundException("Talep", dto.getTalepId()));
            tenantChecker.check(talep.getSirketId(), "Talep");
            if (dto.getSirketId() != null && talep.getSirketId() != null
                    && !dto.getSirketId().equals(talep.getSirketId())) {
                throw new ResourceNotFoundException("Talep bu sirkete ait degil");
            }
            if (!"ONAYLANDI".equals(talep.getDurum())) {
                throw new BusinessException(
                        "Yalnızca onaylanmış talep siparişe dönüştürülebilir (talep durumu: " + talep.getDurum() + ").");
            }
        }

        SatinalmaSiparis s = SatinalmaSiparis.builder()
                .siparisNo(dto.getSiparisNo())
                .tarih(dto.getTarih())
                .cariHesapId(dto.getCariHesapId())
                .talepId(dto.getTalepId())
                .durum("TASLAK")
                .aciklama(dto.getAciklama())
                .sirketId(dto.getSirketId())
                .build();
        // Toplamlar kalemlerden KDV-dahil kanonik modelle hesaplanir (fatura ile uyumlu).
        toplamlariHesapla(s, dto.getKalemler());
        tenantChecker.checkSirketId(dto.getSirketId(), "Satınalma Siparişi");
        s = siparisRepository.save(s);

        if (dto.getKalemler() != null) {
            for (SatinalmaSiparisKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(SatinalmaSiparisKalem.builder()
                        .siparisId(s.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                        .kdvOrani(k.getKdvOrani()).tutar(k.getTutar())
                        .build());
            }
        }
        if (talep != null) {
            talep.setDurum("SIPARISE_DONUSTU");
            talepRepository.save(talep);
        }
        return entityToDTO(s);
    }

    public SatinalmaSiparisDTO guncelle(Long id, SatinalmaSiparisDTO dto) {
        SatinalmaSiparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        if ("FATURALANDI".equals(s.getDurum())) {
            throw new BusinessException("Faturası oluşturulmuş satınalma siparişi doğrudan düzenlenemez");
        }
        donemService.kilitKontrol(s.getSirketId(), s.getTarih() != null ? s.getTarih() : LocalDate.now(), "satınalma siparişi güncelleme");
        s.setSiparisNo(dto.getSiparisNo());
        s.setTarih(dto.getTarih());
        s.setCariHesapId(dto.getCariHesapId());
        s.setTalepId(dto.getTalepId());
        if (dto.getDurum() != null) s.setDurum(dto.getDurum());
        s.setAciklama(dto.getAciklama());
        if (dto.getKalemler() != null) {
            // Kalemler verildiyse toplamlar sunucuda yeniden hesaplanir (istemciye guvenilmez).
            toplamlariHesapla(s, dto.getKalemler());
        }
        s = siparisRepository.save(s);
        if (dto.getKalemler() != null) {
            kalemRepository.deleteBySiparisId(s.getId());
            for (SatinalmaSiparisKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(SatinalmaSiparisKalem.builder()
                        .siparisId(s.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                        .kdvOrani(k.getKdvOrani()).tutar(k.getTutar())
                        .build());
            }
        }
        return entityToDTO(s);
    }

    /** Kalemlerden KDV-dahil kanonik modelle toplamlari hesaplar; bos kalem reddedilir. */
    private void toplamlariHesapla(SatinalmaSiparis s, List<SatinalmaSiparisKalemDTO> kalemler) {
        if (kalemler == null || kalemler.isEmpty()) {
            throw new BusinessException("Satınalma siparişine en az bir kalem eklenmelidir");
        }
        List<com.raspel.erp.util.FaturaTutar.Satir> satirlar = new java.util.ArrayList<>();
        for (SatinalmaSiparisKalemDTO k : kalemler) {
            BigDecimal miktar = k.getMiktar() != null ? k.getMiktar() : BigDecimal.ONE;
            if (miktar.signum() <= 0) {
                throw new BusinessException("Kalem miktarı 0'dan büyük olmalıdır");
            }
            BigDecimal birimFiyat = k.getBirimFiyat() != null ? k.getBirimFiyat() : BigDecimal.ZERO;
            BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : varsayilanKdvOrani;
            com.raspel.erp.util.FaturaTutar.Satir satir =
                    com.raspel.erp.util.FaturaTutar.satir(birimFiyat, miktar, BigDecimal.ZERO, kdvOrani);
            satirlar.add(satir);
            k.setMiktar(miktar);
            k.setKdvOrani(kdvOrani);
            k.setTutar(satir.brut());
        }
        com.raspel.erp.util.FaturaTutar.Belge belge =
                com.raspel.erp.util.FaturaTutar.belge(satirlar, BigDecimal.ZERO);
        s.setAraToplam(belge.araToplam());
        s.setKdv(belge.kdv());
        s.setGenelToplam(belge.genelToplam());
    }

    public SatinalmaSiparisDTO durumGuncelle(Long id, String durum) {
        SatinalmaSiparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        if ("FATURALANDI".equals(s.getDurum()) && !"FATURALANDI".equals(durum)) {
            throw new BusinessException(
                    "Faturası oluşturulmuş satınalma siparişi geri alınamaz; önce alış faturasını iptal edin.");
        }
        if ("TESLIM_ALINDI".equals(durum)) {
            teslimAlStokGirisi(s);
        } else if (Boolean.TRUE.equals(s.getStokIslendi())) {
            // Stok girişi işlenmiş sipariş geri alınırsa stok ters kaydedilir; böylece
            // yanlış "Teslim Al" tıklaması kalıcı stok şişmesi bırakmaz.
            teslimAlStokTersKayit(s);
        }
        s.setDurum(durum);
        return entityToDTO(siparisRepository.save(s));
    }

    /**
     * "Teslim Al" ile işlenen stok girişini tersine çevirir. Stok yetersizse
     * (mal satılmış/tüketilmişse) geri alma engellenir; kullanıcı alış faturası/iade
     * akışını kullanmalıdır.
     */
    private void teslimAlStokTersKayit(SatinalmaSiparis s) {
        List<SatinalmaSiparisKalem> kalemler = kalemRepository.findBySiparisId(s.getId());
        for (SatinalmaSiparisKalem k : kalemler) {
            if (k.getStokId() == null || k.getMiktar() == null
                    || k.getMiktar().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            stokService.hareketEkle(StokHareketDTO.builder()
                    .stokId(k.getStokId())
                    .tur("CIKIS")
                    .miktar(k.getMiktar())
                    .hareketTarihi(LocalDate.now())
                    .cariHesapId(s.getCariHesapId())
                    .aciklama("Satınalma teslim geri alındı: " + s.getSiparisNo())
                    .kaynakTip("SATINALMA_IPTAL")
                    .kaynakId(s.getId())
                    .build());
        }
        s.setStokIslendi(false);
    }

    /**
     * "Teslim Al" adımında satış/alış siparişinin kalemlerini gerçek stok girişi olarak işler.
     * Aynı sipariş için tekrar çalışmaz (stokIslendi bayrağı). Faturaya dönüşümde çift giriş önlenir.
     */
    private void teslimAlStokGirisi(SatinalmaSiparis s) {
        if (Boolean.TRUE.equals(s.getStokIslendi())) {
            return;
        }
        List<SatinalmaSiparisKalem> kalemler = kalemRepository.findBySiparisId(s.getId());
        for (SatinalmaSiparisKalem k : kalemler) {
            if (k.getStokId() == null || k.getMiktar() == null
                    || k.getMiktar().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            stokService.hareketEkle(StokHareketDTO.builder()
                    .stokId(k.getStokId())
                    .tur("GIRIS")
                    .miktar(k.getMiktar())
                    .hareketTarihi(LocalDate.now())
                    .cariHesapId(s.getCariHesapId())
                    .aciklama("Satınalma teslim: " + s.getSiparisNo())
                    .kaynakTip("SATINALMA")
                    .kaynakId(s.getId())
                    .build());
        }
        s.setStokIslendi(true);
    }

    /**
     * Satın alma siparişini alış faturasına dönüştürür.
     * Sipariş kalemleri fatura kalemlerine kopyalanır, fatura KESİLDİ olarak oluşturulur
     * (stok artar + tedarikçi bakiyesi güncellenir) ve sipariş FATURALANDI durumuna geçer.
     */
    public FaturaDTO faturayaCevir(Long id, Long kullaniciId, String displayName) {
        SatinalmaSiparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        if ("FATURALANDI".equals(s.getDurum())) {
            throw new BusinessException("Bu sipariş zaten faturaya dönüştürülmüş");
        }

        List<SatinalmaSiparisKalem> kalemler = kalemRepository.findBySiparisId(id);
        if (kalemler.isEmpty()) {
            throw new BusinessException("Faturaya dönüştürülecek sipariş kalemi yok");
        }

        List<FaturaKalemDTO> faturaKalemleri = kalemler.stream().map(k -> {
            BigDecimal adet = k.getMiktar() != null && k.getMiktar().compareTo(BigDecimal.ZERO) > 0
                    ? k.getMiktar() : BigDecimal.ONE;
            String aciklama = k.getAciklama() != null && !k.getAciklama().isBlank()
                    ? k.getAciklama() : stokAdi(k.getStokId());
            return FaturaKalemDTO.builder()
                    .aciklama(aciklama)
                    .adet(adet)
                    .birimFiyat(k.getBirimFiyat() != null ? k.getBirimFiyat() : BigDecimal.ZERO)
                    .kdvOrani(k.getKdvOrani())
                    .stokId(k.getStokId())
                    .build();
        }).collect(Collectors.toList());

        FaturaDTO faturaDTO = FaturaDTO.builder()
                .tur("ALIS")
                .durum("KESILDI")
                .tarih(LocalDate.now())
                .cariHesapId(s.getCariHesapId())
                .aciklama(s.getAciklama() != null ? s.getAciklama() : "Sipariş: " + s.getSiparisNo())
                .kalemler(faturaKalemleri)
                // Teslim alınmışsa stok girişi orada işlendi; fatura tekrar giriş yapmasın.
                .stokIslemeAtla(Boolean.TRUE.equals(s.getStokIslendi()))
                .build();

        FaturaDTO olusturulan = faturaService.faturaOlustur(faturaDTO, s.getSirketId(), kullaniciId, displayName);

        s.setDurum("FATURALANDI");
        siparisRepository.save(s);

        return olusturulan;
    }

    private String stokAdi(Long stokId) {
        if (stokId == null) return "Ürün";
        return stokRepository.findById(stokId).map(Stok::getAd).orElse("Ürün");
    }

    public void sil(Long id) {
        SatinalmaSiparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        if ("FATURALANDI".equals(s.getDurum())) {
            throw new BusinessException("Faturası oluşturulmuş satınalma siparişi silinemez");
        }
        kalemRepository.deleteBySiparisId(id);
        siparisRepository.deleteById(id);
    }

    private SatinalmaSiparisDTO entityToDTO(SatinalmaSiparis s) {
        List<SatinalmaSiparisKalem> kalemler = kalemRepository.findBySiparisId(s.getId());
        Map<Long, Stok> stokMap = stokMapOlustur(kalemler.stream()
                .map(SatinalmaSiparisKalem::getStokId).collect(Collectors.toSet()));
        Map<Long, CariHesap> cariMap = cariMapOlustur(
                new java.util.HashSet<>(java.util.Collections.singletonList(s.getCariHesapId())));
        return entityToDTO(s, kalemler, stokMap, cariMap);
    }

    private Map<Long, Stok> stokMapOlustur(java.util.Collection<Long> stokIdler) {
        List<Long> gecerli = stokIdler.stream().filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (gecerli.isEmpty()) return java.util.Map.of();
        return stokRepository.findAllById(gecerli).stream()
                .collect(Collectors.toMap(Stok::getId, st -> st, (a, b) -> a));
    }

    private Map<Long, CariHesap> cariMapOlustur(java.util.Collection<Long> cariIdler) {
        List<Long> gecerli = cariIdler.stream().filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (gecerli.isEmpty()) return java.util.Map.of();
        return cariHesapRepository.findAllById(gecerli).stream()
                .collect(Collectors.toMap(CariHesap::getId, c -> c, (a, b) -> a));
    }

    private SatinalmaSiparisDTO entityToDTO(SatinalmaSiparis s, List<SatinalmaSiparisKalem> kalemler,
                                            Map<Long, Stok> stokMap, Map<Long, CariHesap> cariMap) {
        List<SatinalmaSiparisKalemDTO> kalemlerDto = kalemler.stream()
                .map(k -> {
                    Stok stok = k.getStokId() != null ? stokMap.get(k.getStokId()) : null;
                    return SatinalmaSiparisKalemDTO.builder()
                            .id(k.getId()).siparisId(k.getSiparisId()).stokId(k.getStokId())
                            .stokAdi(stok != null ? stok.getAd() : null)
                            .aciklama(k.getAciklama()).miktar(k.getMiktar())
                            .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                            .kdvOrani(k.getKdvOrani()).tutar(k.getTutar())
                            .olusturmaTarihi(k.getOlusturmaTarihi()).build();
                })
                .collect(Collectors.toList());

        CariHesap cari = s.getCariHesapId() != null ? cariMap.get(s.getCariHesapId()) : null;
        return SatinalmaSiparisDTO.builder()
                .id(s.getId()).siparisNo(s.getSiparisNo()).tarih(s.getTarih())
                .cariHesapId(s.getCariHesapId())
                .cariHesapAdi(cari != null ? cari.getAd() : null)
                .talepId(s.getTalepId()).durum(s.getDurum())
                .araToplam(s.getAraToplam()).kdv(s.getKdv()).genelToplam(s.getGenelToplam())
                .aciklama(s.getAciklama()).sirketId(s.getSirketId())
                .stokIslendi(s.getStokIslendi())
                .olusturmaTarihi(s.getOlusturmaTarihi()).kalemler(kalemlerDto).build();
    }
}