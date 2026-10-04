package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.KasaDTO;
import com.raspel.erp.dto.finans.KasaHareketDTO;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.raspel.erp.entity.sistem.GelirGiderKategori;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.BankaHareketi;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.entity.finans.KasaHareket;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.BankaHareketiRepository;
import com.raspel.erp.repository.finans.KasaHareketRepository;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.sistem.KategoriRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class KasaService {

    private final KasaRepository kasaRepository;
    private final KasaHareketRepository kasaHareketRepository;
    private final BankaRepository bankaRepository;
    private final BankaHareketiRepository bankaHareketiRepository;
    private final KategoriRepository kategoriRepository;
    private final TenantChecker tenantChecker;
    private final com.raspel.erp.service.sistem.AuditLogService auditLogService;
    private final com.raspel.erp.config.CacheYardimci cacheYardimci;
    private final com.raspel.erp.service.sistem.DonemService donemService;
    private final com.raspel.erp.repository.finans.HareketRepository hareketRepository;
    private final com.raspel.erp.repository.ticaret.FaturaRepository faturaRepository;

    @Transactional(readOnly = true)
    public Page<KasaDTO> tumKasalarGetir(Long sirketId, Pageable pageable) {
        return kasaRepository.findBySirketId(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public KasaDTO kasaGetir(Long id) {
        Kasa kasa = kasaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", id));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        return entityToDTO(kasa);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public KasaDTO kasaOlustur(KasaDTO dto, Long sirketId) {
        Kasa kasa = Kasa.builder().ad(dto.getAd()).bakiye(dto.getBakiye() != null ? dto.getBakiye() : BigDecimal.ZERO).sirketId(sirketId).build();
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return entityToDTO(kasaRepository.save(kasa));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public KasaDTO kasaGuncelle(Long id, KasaDTO dto) {
        Kasa kasa = kasaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", id));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        kasa.setAd(dto.getAd());
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return entityToDTO(kasaRepository.save(kasa));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void kasaSil(Long id) {
        Kasa kasa = kasaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", id));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        if (kasa.getBakiye() != null && kasa.getBakiye().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("Bakiyesi sıfır olmayan kasa silinemez. Mevcut bakiye: " + kasa.getBakiye() + " ₺");
        }
        if (kasaHareketRepository.countByKasaId(id) > 0)
            throw new BusinessException("Bu kasaya ait hareketler var, önce hareketleri silin");
        kasaRepository.deleteById(id);
        cacheYardimci.commitSonrasiTemizle("dashboard");
    }

    @Transactional(readOnly = true)
    public List<KasaHareketDTO> kasaHareketleriGetir(Long kasaId) {
        Kasa kasa = kasaRepository.findById(kasaId)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", kasaId));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        return kasaHareketRepository.findByKasaIdOrderByHareketTarihiDesc(kasaId)
                .stream().map(this::hareketToDTO).collect(Collectors.toList());
    }

    /**
     * Kasa gün sonu (Z raporu): gün başı/gün içi/gün sonu nakit akışı, tahsilatların
     * ödeme yöntemi kırılımı ve günün satışları. Sunucu tarafında hesaplanır;
     * istemci tarafındaki eksik/sayfalı liste hesaplarına güvenilmez.
     */
    @Transactional(readOnly = true)
    public List<com.raspel.erp.dto.finans.KasaGunSonuDTO> gunSonu(Long sirketId, Long kasaId, java.time.LocalDate tarih) {
        java.time.LocalDate gun = tarih != null ? tarih : java.time.LocalDate.now();
        List<Kasa> kasalar = kasaId != null
                ? List.of(kasaRepository.findById(kasaId)
                        .orElseThrow(() -> new ResourceNotFoundException("Kasa", kasaId)))
                : kasaRepository.findBySirketIdOrderByAd(sirketId);
        if (kasalar.isEmpty()) return List.of();

        List<com.raspel.erp.entity.ticaret.Fatura> gunFaturalari =
                faturaRepository.findBySirketIdAndTarihBetween(sirketId, gun, gun).stream()
                        .filter(f -> f.getTur() == com.raspel.erp.entity.ticaret.Fatura.FaturaTur.SATIS
                                && f.getDurum() != com.raspel.erp.entity.ticaret.Fatura.FaturaDurum.IPTAL)
                        .collect(Collectors.toList());

        List<com.raspel.erp.dto.finans.KasaGunSonuDTO> sonuc = new java.util.ArrayList<>();
        for (Kasa kasa : kasalar) {
            tenantChecker.check(kasa.getSirketId(), "Kasa");
            List<KasaHareket> tumu = kasaHareketRepository.findByKasaIdOrderByHareketTarihiDesc(kasa.getId());

            BigDecimal acilis = BigDecimal.ZERO;
            BigDecimal giris = BigDecimal.ZERO;
            BigDecimal cikis = BigDecimal.ZERO;
            List<KasaHareket> gunun = new java.util.ArrayList<>();
            for (KasaHareket kh : tumu) {
                if (kh.getHareketTarihi() == null) continue;
                BigDecimal t = kh.getTutar() != null ? kh.getTutar() : BigDecimal.ZERO;
                boolean gelir = "GELIR".equals(kh.getTur());
                if (kh.getHareketTarihi().isBefore(gun)) {
                    acilis = acilis.add(gelir ? t : t.negate());
                } else if (gun.equals(kh.getHareketTarihi())) {
                    if (gelir) giris = giris.add(t);
                    else cikis = cikis.add(t);
                    gunun.add(kh);
                }
            }

            // Ödeme yöntemi çözümü: kaynakTip=TAHSILAT -> cari hareket, fatura bağlı satır -> fatura.
            List<Long> cariIdler = gunun.stream()
                    .filter(kh -> "TAHSILAT".equals(kh.getKaynakTip()) && kh.getKaynakId() != null)
                    .map(KasaHareket::getKaynakId).distinct().collect(Collectors.toList());
            Map<Long, String> cariYontem = cariIdler.isEmpty() ? Map.of()
                    : hareketRepository.findAllById(cariIdler).stream()
                            .collect(Collectors.toMap(Hareket::getId,
                                    h -> h.getOdemeYontemi() != null
                                            ? h.getOdemeYontemi().toUpperCase(java.util.Locale.ROOT) : "DIGER",
                                    (a, b) -> a));
            List<Long> faturaIdler = gunun.stream()
                    .filter(kh -> kh.getFaturaId() != null)
                    .map(KasaHareket::getFaturaId).distinct().collect(Collectors.toList());
            Map<Long, String> faturaYontem = faturaIdler.isEmpty() ? Map.of()
                    : faturaRepository.findAllById(faturaIdler).stream()
                            .collect(Collectors.toMap(com.raspel.erp.entity.ticaret.Fatura::getId,
                                    f -> f.getOdemeYontemi() != null
                                            ? f.getOdemeYontemi().toUpperCase(java.util.Locale.ROOT) : "DIGER",
                                    (a, b) -> a));

            BigDecimal tahsilatToplam = BigDecimal.ZERO;
            BigDecimal nakit = BigDecimal.ZERO;
            BigDecimal kart = BigDecimal.ZERO;
            BigDecimal havale = BigDecimal.ZERO;
            BigDecimal taksit = BigDecimal.ZERO;
            BigDecimal diger = BigDecimal.ZERO;
            long tahsilatAdedi = 0;
            BigDecimal giderToplam = BigDecimal.ZERO;
            long giderAdedi = 0;
            List<com.raspel.erp.dto.finans.KasaGunSonuDTO.Satir> satirlar = new java.util.ArrayList<>();
            for (KasaHareket kh : gunun) {
                BigDecimal t = kh.getTutar() != null ? kh.getTutar() : BigDecimal.ZERO;
                String yontem = null;
                if ("TAHSILAT".equals(kh.getKaynakTip()) && kh.getKaynakId() != null) {
                    yontem = cariYontem.get(kh.getKaynakId());
                } else if (kh.getFaturaId() != null) {
                    yontem = faturaYontem.get(kh.getFaturaId());
                }
                if ("GELIR".equals(kh.getTur())
                        && ("TAHSILAT".equals(kh.getKaynakTip()) || kh.getFaturaId() != null)) {
                    tahsilatToplam = tahsilatToplam.add(t);
                    tahsilatAdedi++;
                    switch (yontem != null ? yontem : "DIGER") {
                        case "NAKIT" -> nakit = nakit.add(t);
                        case "KART" -> kart = kart.add(t);
                        case "HAVALE" -> havale = havale.add(t);
                        case "TAKSIT" -> taksit = taksit.add(t);
                        default -> diger = diger.add(t);
                    }
                } else if ("GIDER".equals(kh.getTur())) {
                    giderToplam = giderToplam.add(t);
                    giderAdedi++;
                }
                satirlar.add(com.raspel.erp.dto.finans.KasaGunSonuDTO.Satir.builder()
                        .id(kh.getId()).tur(kh.getTur()).tutar(t)
                        .aciklama(kh.getAciklama()).kaynakTip(kh.getKaynakTip())
                        .odemeYontemi(yontem).build());
            }

            long satisAdedi = 0;
            BigDecimal satisToplam = BigDecimal.ZERO;
            for (com.raspel.erp.entity.ticaret.Fatura f : gunFaturalari) {
                if (f.getKasaId() != null && f.getKasaId().equals(kasa.getId())) {
                    satisAdedi++;
                    satisToplam = satisToplam.add(f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO);
                }
            }

            sonuc.add(com.raspel.erp.dto.finans.KasaGunSonuDTO.builder()
                    .tarih(gun).kasaId(kasa.getId()).kasaAd(kasa.getAd())
                    .acilisBakiye(acilis).gunIciGiris(giris).gunIciCikis(cikis)
                    .kapanisBakiye(acilis.add(giris).subtract(cikis))
                    .tahsilatToplam(tahsilatToplam).tahsilatAdedi(tahsilatAdedi)
                    .nakitTahsilat(nakit).kartTahsilat(kart).havaleTahsilat(havale)
                    .taksitTahsilat(taksit).digerTahsilat(diger)
                    .giderToplam(giderToplam).giderAdedi(giderAdedi)
                    .satisAdedi(satisAdedi).satisToplam(satisToplam)
                    .hareketler(satirlar)
                    .build());
        }
        return sonuc;
    }

    public KasaHareketDTO hareketEkle(KasaHareketDTO dto) {
        Kasa kasa = kasaRepository.findByIdForUpdate(dto.getKasaId())
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", dto.getKasaId()));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        donemService.kilitKontrol(kasa.getSirketId(),
                dto.getHareketTarihi() != null ? dto.getHareketTarihi() : java.time.LocalDate.now(), "kasa hareketi ekleme");

        BigDecimal tutar = dto.getTutar();
        if ("GIDER".equals(dto.getTur())) tutar = tutar.negate();
        kasa.setBakiye(kasa.getBakiye().add(tutar));

        GelirGiderKategori kategori = null;
        if (dto.getKategoriId() != null) {
            kategori = kategoriRepository.findById(dto.getKategoriId()).orElse(null);
        }

        KasaHareket hareket = KasaHareket.builder()
                .kasa(kasa).tur(dto.getTur()).tutar(dto.getTutar())
                .hareketTarihi(dto.getHareketTarihi()).aciklama(dto.getAciklama())
                .kategori(kategori).build();

        kasaRepository.save(kasa);
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return hareketToDTO(kasaHareketRepository.save(hareket));
    }

        public void hareketSil(Long hareketId) {
        KasaHareket hareket = kasaHareketRepository.findById(hareketId)
                .orElseThrow(() -> new ResourceNotFoundException("Hareket", hareketId));
        // Kasa satırını kilitle: eşzamanlı silme/ekleme sırasında bakiye kaybını önler.
        Kasa kasa = kasaRepository.findByIdForUpdate(hareket.getKasa().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", hareket.getKasa().getId()));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        donemService.kilitKontrol(kasa.getSirketId(), hareket.getHareketTarihi(), "kasa hareketi silme");
        BigDecimal tutar = hareket.getTutar();
        if ("GIDER".equals(hareket.getTur())) tutar = tutar.negate();
        kasa.setBakiye(kasa.getBakiye().subtract(tutar));
        kasaRepository.save(kasa);
        auditLogService.finansalSilmeLog("KasaHareket", hareketId,
                "Kasa hareketi silindi: " + hareket.getTur() + " " + hareket.getTutar() + " TL - Kasa: "
                        + kasa.getAd() + " (bakiye terslendi)");
        kasaHareketRepository.deleteById(hareketId);
        cacheYardimci.commitSonrasiTemizle("dashboard");
    }

    /**
     * Kasalar arası para aktarımı. Kaynak kasadan düşer, hedef kasaya ekler.
     * Her iki taraf için de hareket kaydı oluşturur.
     */
    public void kasaAktar(Long kaynakKasaId, Long hedefKasaId, BigDecimal tutar, String aciklama, Long sirketId) {
        if (kaynakKasaId.equals(hedefKasaId)) {
            throw new BusinessException("Kaynak ve hedef kasa aynı olamaz");
        }
        if (tutar == null || tutar.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Aktarılacak tutar sıfırdan büyük olmalıdır");
        }

        Kasa kaynak = kasaRepository.findByIdForUpdate(kaynakKasaId)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", kaynakKasaId));
        tenantChecker.check(kaynak.getSirketId(), "Kasa");
        Kasa hedef = kasaRepository.findByIdForUpdate(hedefKasaId)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", hedefKasaId));
        tenantChecker.check(hedef.getSirketId(), "Kasa");

        if (kaynak.getBakiye().compareTo(tutar) < 0) {
            throw new BusinessException("Kaynak kasada yetersiz bakiye. Mevcut: " + kaynak.getBakiye() + " ₺");
        }

        java.time.LocalDate bugun = java.time.LocalDate.now();
        donemService.kilitKontrol(kaynak.getSirketId(), bugun, "kasa aktarımı");
        kaynak.setBakiye(kaynak.getBakiye().subtract(tutar));
        hedef.setBakiye(hedef.getBakiye().add(tutar));

        String not = aciklama != null && !aciklama.isBlank() ? aciklama : "Kasa aktarımı";

        kasaRepository.save(kaynak);
        kasaRepository.save(hedef);

        kasaHareketRepository.save(KasaHareket.builder()
                .kasa(kaynak).tur("GIDER").tutar(tutar)
                .hareketTarihi(bugun).aciklama(not + " → " + hedef.getAd())
                .build());
        kasaHareketRepository.save(KasaHareket.builder()
                .kasa(hedef).tur("GELIR").tutar(tutar)
                .hareketTarihi(bugun).aciklama(not + " ← " + kaynak.getAd())
                .build());

        cacheYardimci.commitSonrasiTemizle("dashboard");
        log.info("Kasa aktarımı yapıldı: {} → {} ({} ₺)", kaynak.getAd(), hedef.getAd(), tutar);
    }

    /**
     * Kasadan banka hesabına para aktarımı. Kasadan düşer, banka bakiyesine ekler.
     */
    public void kasaBankayaAktar(Long kasaId, Long bankaId, BigDecimal tutar, String aciklama, Long sirketId) {
        if (tutar == null || tutar.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Aktarılacak tutar sıfırdan büyük olmalıdır");
        }
        Kasa kasa = kasaRepository.findByIdForUpdate(kasaId)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", kasaId));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        Banka banka = bankaRepository.findByIdForUpdate(bankaId)
                .orElseThrow(() -> new ResourceNotFoundException("Banka", bankaId));
        tenantChecker.check(banka.getSirketId(), "Banka");

        if (kasa.getBakiye().compareTo(tutar) < 0) {
            throw new BusinessException("Kasada yetersiz bakiye. Mevcut: " + kasa.getBakiye() + " ₺");
        }

        kasa.setBakiye(kasa.getBakiye().subtract(tutar));
        banka.setBakiye(banka.getBakiye() != null ? banka.getBakiye().add(tutar) : tutar);

        java.time.LocalDate bugun = java.time.LocalDate.now();
        donemService.kilitKontrol(kasa.getSirketId(), bugun, "kasa-banka aktarımı");
        String ek = aciklama != null && !aciklama.isBlank() ? " (" + aciklama + ")" : "";

        kasaRepository.save(kasa);
        bankaRepository.save(banka);
        kasaHareketRepository.save(KasaHareket.builder()
                .kasa(kasa).tur("GIDER").tutar(tutar)
                .hareketTarihi(bugun).aciklama("Kasadan bankaya aktarıldı → " + banka.getAd() + ek)
                .build());
        // Kaynak izi: banka tarafina da hareket yaz (banka ekstresi/mutabakat gorunsun).
        bankaHareketiRepository.save(BankaHareketi.builder()
                .bankaId(banka.getId())
                .tarih(bugun)
                .aciklama("Kasadan aktarıldı: " + kasa.getAd() + ek)
                .borc(BigDecimal.ZERO)
                .alacak(tutar)
                .bakiye(banka.getBakiye())
                .eslestirildi(false)
                .sirketId(kasa.getSirketId())
                .build());

        cacheYardimci.commitSonrasiTemizle("dashboard");
        log.info("Kasa → Banka aktarımı: {} → {} ({} ₺)", kasa.getAd(), banka.getAd(), tutar);
    }

    /**
     * Banka hesabından kasaya para aktarımı. Bankadan düşer, kasa bakiyesine ekler.
     */
    public void bankaKasayaAktar(Long bankaId, Long kasaId, BigDecimal tutar, String aciklama, Long sirketId) {
        if (tutar == null || tutar.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Aktarılacak tutar sıfırdan büyük olmalıdır");
        }
        Banka banka = bankaRepository.findByIdForUpdate(bankaId)
                .orElseThrow(() -> new ResourceNotFoundException("Banka", bankaId));
        tenantChecker.check(banka.getSirketId(), "Banka");
        Kasa kasa = kasaRepository.findByIdForUpdate(kasaId)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", kasaId));
        tenantChecker.check(kasa.getSirketId(), "Kasa");

        BigDecimal bankaBakiye = banka.getBakiye() != null ? banka.getBakiye() : BigDecimal.ZERO;
        if (bankaBakiye.compareTo(tutar) < 0) {
            throw new BusinessException("Bankada yetersiz bakiye. Mevcut: " + bankaBakiye + " ₺");
        }

        banka.setBakiye(bankaBakiye.subtract(tutar));
        kasa.setBakiye(kasa.getBakiye() != null ? kasa.getBakiye().add(tutar) : tutar);

        java.time.LocalDate bugun = java.time.LocalDate.now();
        donemService.kilitKontrol(kasa.getSirketId(), bugun, "banka-kasa aktarımı");
        String ek = aciklama != null && !aciklama.isBlank() ? " (" + aciklama + ")" : "";

        bankaRepository.save(banka);
        kasaRepository.save(kasa);
        bankaHareketiRepository.save(BankaHareketi.builder()
                .bankaId(banka.getId())
                .tarih(bugun)
                .aciklama("Bankadan kasaya aktarıldı: " + kasa.getAd() + ek)
                .borc(tutar)
                .alacak(BigDecimal.ZERO)
                .bakiye(banka.getBakiye())
                .eslestirildi(false)
                .sirketId(kasa.getSirketId())
                .build());
        kasaHareketRepository.save(KasaHareket.builder()
                .kasa(kasa).tur("GELIR").tutar(tutar)
                .hareketTarihi(bugun).aciklama("Bankadan aktarıldı ← " + banka.getAd() + ek)
                .build());

        cacheYardimci.commitSonrasiTemizle("dashboard");
        log.info("Banka → Kasa aktarımı: {} → {} ({} ₺)", banka.getAd(), kasa.getAd(), tutar);
    }

    private KasaDTO entityToDTO(Kasa k) {
        return KasaDTO.builder().id(k.getId()).ad(k.getAd()).bakiye(k.getBakiye())
                .olusturmaTarihi(k.getOlusturmaTarihi()).build();
    }

    private KasaHareketDTO hareketToDTO(KasaHareket h) {
        return KasaHareketDTO.builder()
                .id(h.getId()).kasaId(h.getKasa().getId()).kasaAd(h.getKasa().getAd())
                .tur(h.getTur()).tutar(h.getTutar()).hareketTarihi(h.getHareketTarihi())
                .aciklama(h.getAciklama())
                .kategoriId(h.getKategori() != null ? h.getKategori().getId() : null)
                .kategoriAd(h.getKategori() != null ? h.getKategori().getAd() : null)
                .olusturmaTarihi(h.getOlusturmaTarihi()).build();
    }
}