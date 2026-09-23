package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.ticaret.IadeDTO;
import com.raspel.erp.dto.ticaret.IadeKalemDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.entity.ticaret.Iade;
import com.raspel.erp.entity.ticaret.IadeKalem;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.IadeKalemRepository;
import com.raspel.erp.repository.ticaret.IadeRepository;
import com.raspel.erp.service.finans.CariHesapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class IadeService {

    private final IadeRepository iadeRepository;
    private final IadeKalemRepository iadeKalemRepository;
    private final StokRepository stokRepository;
    private final StokHareketRepository stokHareketRepository;
    private final FaturaRepository faturaRepository;
    private final com.raspel.erp.repository.ticaret.FaturaKalemRepository faturaKalemRepository;
    private final CariHesapService cariHesapService;
    private final com.raspel.erp.service.sube.DepoStokService depoStokService;
    private final com.raspel.erp.service.envanter.MaliyetService maliyetService;
    private final TenantChecker tenantChecker;
    private final CacheYardimci cacheYardimci;
    private final com.raspel.erp.service.sistem.DonemService donemService;

    @org.springframework.beans.factory.annotation.Value("${app.kdv.varsayilan-oran:20}")
    private BigDecimal varsayilanKdvOrani;

    @Transactional(readOnly = true)
    public Page<IadeDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return iadeRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public IadeDTO getir(Long id) {
        Iade iade = iadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Iade", id));
        tenantChecker.check(iade.getSirketId(), "Iade");
        return entityToDTO(iade);
    }

    public IadeDTO olustur(IadeDTO dto, Long sirketId) {
        donemService.kilitKontrol(sirketId,
                dto.getTarih() != null ? dto.getTarih() : LocalDate.now(), "iade oluşturma");
        iadeSiniriDogrula(dto);
        BigDecimal toplamTutar = BigDecimal.ZERO;
        if (dto.getKalemler() != null && !dto.getKalemler().isEmpty()) {
            for (IadeKalemDTO k : dto.getKalemler()) {
                BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : varsayilanKdvOrani;
                BigDecimal netTutar = k.getBirimFiyat().multiply(k.getMiktar());
                BigDecimal kdvTutari = netTutar.multiply(kdvOrani).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal kalemTutar = netTutar.add(kdvTutari);
                toplamTutar = toplamTutar.add(kalemTutar);
            }
        } else if (dto.getTutar() != null) {
            toplamTutar = dto.getTutar();
        }

        Iade iade = Iade.builder()
                .faturaId(dto.getFaturaId())
                .tur(dto.getTur() != null ? dto.getTur() : "SATIS")
                .tarih(dto.getTarih())
                .tutar(toplamTutar)
                .aciklama(dto.getAciklama())
                .durum(dto.getDurum() != null ? dto.getDurum() : "TASLAK")
                .sirketId(sirketId)
                .build();
        iade = iadeRepository.save(iade);

        if (dto.getKalemler() != null) {
            for (IadeKalemDTO k : dto.getKalemler()) {
                stokTenantDogrula(k.getStokId(), sirketId);
                BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : varsayilanKdvOrani;
                BigDecimal netTutar = k.getBirimFiyat().multiply(k.getMiktar());
                BigDecimal kdvTutari = netTutar.multiply(kdvOrani).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                iadeKalemRepository.save(IadeKalem.builder()
                        .iadeId(iade.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                        .kdvOrani(kdvOrani).tutar(netTutar.add(kdvTutari))
                        .build());
            }
        }

        if ("TAMAMLANDI".equals(iade.getDurum())) {
            stokHareketleriIsle(iade);
        }

        return entityToDTO(iade);
    }

    /**
     * İade, bağlı faturayı aşamaz: kalem bazında iade miktarı fatura kalem miktarını,
     * toplam iade tutarı da (önceki iadeler dahil) fatura genel toplamını geçemez.
     * Faturasız iadeler için yalnızca pozitiflik kontrolü yapılır.
     */
    private void iadeSiniriDogrula(IadeDTO dto) {
        if (dto.getFaturaId() == null) return;
        Fatura fatura = faturaRepository.findById(dto.getFaturaId()).orElse(null);
        if (fatura == null) {
            throw new ResourceNotFoundException("Fatura", dto.getFaturaId());
        }
        tenantChecker.check(fatura.getSirketId(), "Fatura");

        // Bu faturaya bağlı, iptal edilmemiş önceki iadeler (miktar ve tutar sınırı için).
        List<Iade> oncekiIadeListesi = iadeRepository
                .findByFaturaIdInAndSirketId(List.of(fatura.getId()), fatura.getSirketId())
                .stream().filter(i -> !"IPTAL".equals(i.getDurum())).collect(Collectors.toList());

        // Kalem bazlı üst sınır: (önceki iadeler + bu iade) ilgili fatura kalem miktarını aşamaz.
        if (dto.getKalemler() != null && !dto.getKalemler().isEmpty()) {
            Map<Long, BigDecimal> faturaMiktarlari = new java.util.HashMap<>();
            for (com.raspel.erp.entity.ticaret.FaturaKalem fk : faturaKalemRepository.findByFaturaId(fatura.getId())) {
                if (fk.getStokId() == null) continue;
                faturaMiktarlari.merge(fk.getStokId(), fk.getAdet() != null ? fk.getAdet() : BigDecimal.ZERO, BigDecimal::add);
            }
            Map<Long, BigDecimal> oncekiIadeMiktarlari = new java.util.HashMap<>();
            for (Iade onceki : oncekiIadeListesi) {
                for (IadeKalem ik : iadeKalemRepository.findByIadeId(onceki.getId())) {
                    if (ik.getStokId() == null) continue;
                    oncekiIadeMiktarlari.merge(ik.getStokId(), ik.getMiktar() != null ? ik.getMiktar() : BigDecimal.ZERO, BigDecimal::add);
                }
            }
            Map<Long, BigDecimal> iadeMiktarlari = new java.util.HashMap<>();
            for (IadeKalemDTO k : dto.getKalemler()) {
                if (k.getStokId() == null) continue;
                iadeMiktarlari.merge(k.getStokId(), k.getMiktar() != null ? k.getMiktar() : BigDecimal.ZERO, BigDecimal::add);
            }
            for (Map.Entry<Long, BigDecimal> e : iadeMiktarlari.entrySet()) {
                BigDecimal satisMiktari = faturaMiktarlari.getOrDefault(e.getKey(), BigDecimal.ZERO);
                BigDecimal toplamIade = oncekiIadeMiktarlari.getOrDefault(e.getKey(), BigDecimal.ZERO).add(e.getValue());
                if (toplamIade.compareTo(satisMiktari) > 0) {
                    throw new BusinessException("Toplam iade miktarı faturadaki satış miktarını aşamaz (stok: "
                            + e.getKey() + ", satış: " + satisMiktari + ", toplam iade: " + toplamIade + ")");
                }
            }
        }

        // Tutar bazlı üst sınır: önceki iadeler + bu iade <= fatura toplamı.
        BigDecimal yeniTutar = dto.getTutar() != null ? dto.getTutar() : BigDecimal.ZERO;
        if (dto.getKalemler() != null && !dto.getKalemler().isEmpty()) {
            yeniTutar = BigDecimal.ZERO;
            for (IadeKalemDTO k : dto.getKalemler()) {
                BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : varsayilanKdvOrani;
                BigDecimal net = k.getBirimFiyat().multiply(k.getMiktar());
                BigDecimal kdv = net.multiply(kdvOrani).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                yeniTutar = yeniTutar.add(net.add(kdv));
            }
        }
        BigDecimal oncekiIadeler = oncekiIadeListesi.stream()
                .map(i -> i.getTutar() != null ? i.getTutar() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal faturaToplam = fatura.getGenelToplam() != null ? fatura.getGenelToplam() : BigDecimal.ZERO;
        if (oncekiIadeler.add(yeniTutar).compareTo(faturaToplam) > 0) {
            throw new BusinessException("Toplam iade tutarı fatura tutarını aşamaz (fatura: "
                    + faturaToplam + ", önceki iadeler: " + oncekiIadeler + ", bu iade: " + yeniTutar + ")");
        }
    }

    public IadeDTO guncelle(Long id, IadeDTO dto) {
        Iade iade = iadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Iade", id));
        tenantChecker.check(iade.getSirketId(), "Iade");
        donemService.kilitKontrol(iade.getSirketId(), iade.getTarih(), "iade düzenleme");
        if (dto.getTarih() != null) {
            donemService.kilitKontrol(iade.getSirketId(), dto.getTarih(), "iade düzenleme");
        }
        String eskiDurum = iade.getDurum();

        if ("TAMAMLANDI".equals(eskiDurum)) {
            if (dto.getKalemler() != null && !dto.getKalemler().isEmpty()) {
                throw new BusinessException("Tamamlanmış iadenin kalemleri değiştirilemez. Önce iadeyi iptal edin.");
            }
            if (dto.getTutar() != null && dto.getTutar().compareTo(iade.getTutar()) != 0) {
                throw new BusinessException("Tamamlanmış iadenin tutarı değiştirilemez. Önce iadeyi iptal edin.");
            }
            if ("TASLAK".equals(dto.getDurum())) {
                throw new BusinessException("Tamamlanmış iade TASLAK'a alınamaz. Yalnızca IPTAL edilebilir.");
            }
        }

        if (dto.getFaturaId() != null) iade.setFaturaId(dto.getFaturaId());
        if (dto.getTarih() != null) iade.setTarih(dto.getTarih());
        if (dto.getAciklama() != null) iade.setAciklama(dto.getAciklama());
        if (dto.getDurum() != null) iade.setDurum(dto.getDurum());

        BigDecimal toplamTutar = BigDecimal.ZERO;
        if (dto.getKalemler() != null && !dto.getKalemler().isEmpty()) {
            iadeKalemRepository.deleteByIadeId(iade.getId());
            for (IadeKalemDTO k : dto.getKalemler()) {
                stokTenantDogrula(k.getStokId(), iade.getSirketId());
                BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : varsayilanKdvOrani;
                BigDecimal netTutar = k.getBirimFiyat().multiply(k.getMiktar());
                BigDecimal kdvTutari = netTutar.multiply(kdvOrani).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal kalemTutar = netTutar.add(kdvTutari);
                toplamTutar = toplamTutar.add(kalemTutar);
                iadeKalemRepository.save(IadeKalem.builder()
                        .iadeId(iade.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                        .kdvOrani(kdvOrani).tutar(kalemTutar)
                        .build());
            }
            iade.setTutar(toplamTutar);
        } else if (dto.getTutar() != null) {
            iade.setTutar(dto.getTutar());
        }

        boolean yeniTamamlandi = "TAMAMLANDI".equals(iade.getDurum()) && !"TAMAMLANDI".equals(eskiDurum);
        boolean iptalEdildi = "IPTAL".equals(iade.getDurum()) && "TAMAMLANDI".equals(eskiDurum);
        if (yeniTamamlandi) {
            stokHareketleriIsle(iade);
            cacheYardimci.temizle("stoklar", "dashboard");
        } else if (iptalEdildi) {
            stokHareketleriniTersineCevir(iade);
            cacheYardimci.temizle("stoklar", "dashboard");
        }

        return entityToDTO(iadeRepository.save(iade));
    }

    public void sil(Long id) {
        Iade iade = iadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Iade", id));
        tenantChecker.check(iade.getSirketId(), "Iade");
        if ("TAMAMLANDI".equals(iade.getDurum())) {
            throw new BusinessException("Tamamlanmış iade doğrudan silinemez, önce iptal edilmelidir");
        }
        iadeKalemRepository.deleteByIadeId(id);
        iadeRepository.deleteById(id);
    }

    public IadeDTO durumGuncelle(Long id, String yeniDurum) {
        Iade iade = iadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Iade", id));
        tenantChecker.check(iade.getSirketId(), "Iade");
        donemService.kilitKontrol(iade.getSirketId(), iade.getTarih(), "iade durum güncelleme");
        if (yeniDurum == null || !List.of("TASLAK", "TAMAMLANDI", "IPTAL").contains(yeniDurum)) {
            throw new BusinessException("Geçersiz durum: " + yeniDurum);
        }
        if ("TAMAMLANDI".equals(yeniDurum) && !"TAMAMLANDI".equals(iade.getDurum())) {
            stokHareketleriIsle(iade);
            cacheYardimci.temizle("stoklar", "dashboard");
        } else if ("IPTAL".equals(yeniDurum) && "TAMAMLANDI".equals(iade.getDurum())) {
            stokHareketleriniTersineCevir(iade);
            cacheYardimci.temizle("stoklar", "dashboard");
        }
        iade.setDurum(yeniDurum);
        return entityToDTO(iadeRepository.save(iade));
    }

    private void stokHareketleriIsle(Iade iade) {
        boolean alisIadesi = "ALIS".equals(iade.getTur());
        List<IadeKalem> kalemler = iadeKalemRepository.findByIadeId(iade.getId());
        Long depoId = depoIdBul(iade);
        for (IadeKalem k : kalemler) {
            if (k.getStokId() == null) continue;
            Stok stok = stokRepository.findByIdForUpdate(k.getStokId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stok", k.getStokId()));
            // Güvenlik: iadenin şirketi dışındaki stoklara dokunulamaz.
            tenantChecker.check(stok.getSirketId(), "Stok");
            if (iade.getSirketId() != null && stok.getSirketId() != null && !iade.getSirketId().equals(stok.getSirketId())) {
                throw new ResourceNotFoundException("Stok bu sirkete ait degil");
            }
            if (alisIadesi) {
                if (stok.getMiktar().compareTo(k.getMiktar()) < 0)
                    throw new BusinessException("Yetersiz stok! Ürün: " + stok.getAd() + ", Mevcut: " + stok.getMiktar());
                stok.setMiktar(stok.getMiktar().subtract(k.getMiktar()));
                maliyetService.cikisIsle(stok, k.getMiktar(), stok.getMiktar(), iade.getSirketId(), "IADE", iade.getId());
            } else {
                BigDecimal eskiMiktar = stok.getMiktar() != null ? stok.getMiktar() : BigDecimal.ZERO;
                stok.setMiktar(stok.getMiktar().add(k.getMiktar()));
                maliyetService.girisIsle(stok, eskiMiktar, k.getMiktar(), null, iade.getSirketId(), "IADE", iade.getId());
            }
            stokRepository.save(stok);
            stokHareketRepository.save(StokHareket.builder()
                    .stok(stok).tur(alisIadesi ? "CIKIS" : "GIRIS")
                    .miktar(k.getMiktar())
                    .hareketTarihi(LocalDate.now())
                    .aciklama("İade #" + iade.getId())
                    .depoId(depoId)
                    .kaynakTip("IADE").kaynakId(iade.getId())
                    .build());
            depoStokService.guncelle(depoId, stok.getId(),
                    alisIadesi ? k.getMiktar().negate() : k.getMiktar());
        }
        cariBakiyeUygula(iade, false);
    }

    /** İade kaleminde gönderilen stoğun iade şirketine ait olduğunu doğrular. */
    private void stokTenantDogrula(Long stokId, Long sirketId) {
        if (stokId == null) return;
        stokRepository.findById(stokId).ifPresent(stok -> {
            tenantChecker.check(stok.getSirketId(), "Stok");
            if (sirketId != null && stok.getSirketId() != null && !sirketId.equals(stok.getSirketId())) {
                throw new ResourceNotFoundException("Stok bu sirkete ait degil");
            }
        });
    }

    /** Iadeye bagli faturanin deposu; yoksa varsayilan aktif depo. */
    private Long depoIdBul(Iade iade) {
        Long depoId = null;
        if (iade.getFaturaId() != null) {
            depoId = faturaRepository.findById(iade.getFaturaId()).map(Fatura::getDepoId).orElse(null);
        }
        return depoStokService.coz(depoId, iade.getSirketId());
    }

    private void stokHareketleriniTersineCevir(Iade iade) {
        boolean alisIadesi = "ALIS".equals(iade.getTur());
        List<IadeKalem> kalemler = iadeKalemRepository.findByIadeId(iade.getId());
        Long depoId = depoIdBul(iade);
        for (IadeKalem k : kalemler) {
            if (k.getStokId() == null) continue;
            Stok stok = stokRepository.findByIdForUpdate(k.getStokId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stok", k.getStokId()));
            // Güvenlik: iadenin şirketi dışındaki stoklara dokunulamaz.
            tenantChecker.check(stok.getSirketId(), "Stok");
            if (iade.getSirketId() != null && stok.getSirketId() != null && !iade.getSirketId().equals(stok.getSirketId())) {
                throw new ResourceNotFoundException("Stok bu sirkete ait degil");
            }
            if (alisIadesi) {
                BigDecimal eskiMiktar = stok.getMiktar() != null ? stok.getMiktar() : BigDecimal.ZERO;
                stok.setMiktar(stok.getMiktar().add(k.getMiktar()));
                maliyetService.girisIsle(stok, eskiMiktar, k.getMiktar(), null, iade.getSirketId(), "IADE", iade.getId());
            } else {
                if (stok.getMiktar().compareTo(k.getMiktar()) < 0)
                    throw new BusinessException("Yetersiz stok! Ürün: " + stok.getAd() + ", Mevcut: " + stok.getMiktar());
                stok.setMiktar(stok.getMiktar().subtract(k.getMiktar()));
                maliyetService.cikisIsle(stok, k.getMiktar(), stok.getMiktar(), iade.getSirketId(), "IADE", iade.getId());
            }
            stokRepository.save(stok);
            stokHareketRepository.save(StokHareket.builder()
                    .stok(stok).tur(alisIadesi ? "GIRIS" : "CIKIS")
                    .miktar(k.getMiktar())
                    .hareketTarihi(LocalDate.now())
                    .aciklama("İade iptal #" + iade.getId())
                    .depoId(depoId)
                    .kaynakTip("IADE").kaynakId(iade.getId())
                    .build());
            depoStokService.guncelle(depoId, stok.getId(),
                    alisIadesi ? k.getMiktar() : k.getMiktar().negate());
        }
        cariBakiyeUygula(iade, true);
    }

    /**
     * Iade tamamlandiginda cari bakiyeye etki uygular; iptal edilince tersine cevirir.
     * Satis iadesi musterinin borcunu azaltir (+), alis iadesi tedarikci alacagini azaltir (-).
     * Yalnizca faturaya bagli iadelerde uygulanir.
     */
    private void cariBakiyeUygula(Iade iade, boolean ters) {
        if (iade.getFaturaId() == null) return;
        Fatura fatura = faturaRepository.findById(iade.getFaturaId()).orElse(null);
        if (fatura == null || fatura.getCariHesap() == null) return;
        BigDecimal iadeTutar = iade.getTutar() != null ? iade.getTutar() : BigDecimal.ZERO;

        // Bağlı faturanın kalan/ödenen tutarını da güncelle; aksi halde iade sonrası
        // fatura hâlâ tam borçlu görünür.
        faturaKalanGuncelle(fatura, ters ? iadeTutar.negate() : iadeTutar);

        BigDecimal tutar = iadeTutar;
        if (!"SATIS".equals(iade.getTur())) {
            tutar = tutar.negate();
        }
        if (ters) {
            tutar = tutar.negate();
        }
        cariHesapService.bakiyeGuncelle(fatura.getCariHesap().getId(), tutar);
    }

    /**
     * İade nedeniyle faturanın KALAN tutarını delta kadar değiştirir (delta: iade
     * tamamlandığında +iadeTutar, iptalinde -iadeTutar). Ödenen tutar şişirilmez;
     * iade, ödenmiş faturada müşteri lehine alacak olarak cariye yansır.
     */
    private void faturaKalanGuncelle(Fatura fatura, BigDecimal delta) {
        if (delta == null || delta.signum() == 0) return;
        BigDecimal toplam = fatura.getGenelToplam() != null ? fatura.getGenelToplam() : BigDecimal.ZERO;
        BigDecimal mevcutKalan = fatura.getKalanTutar() != null ? fatura.getKalanTutar() : toplam;
        BigDecimal kalan = mevcutKalan.subtract(delta);
        if (kalan.signum() < 0) kalan = BigDecimal.ZERO;
        if (kalan.compareTo(toplam) > 0) kalan = toplam;
        fatura.setKalanTutar(kalan);

        BigDecimal odenen = fatura.getOdenenTutar() != null ? fatura.getOdenenTutar() : BigDecimal.ZERO;
        if (odenen.compareTo(toplam) > 0) {
            odenen = toplam;
            fatura.setOdenenTutar(odenen);
        }
        // Kalan 0 ise fatura kapanmıştır (ödeme veya iade ile) -> ODENDI.
        fatura.setOdemeDurumu(kalan.signum() <= 0 ? "ODENDI"
                : odenen.signum() > 0 ? "KISMI_ODENDI" : "ODENMEDI");
        faturaRepository.save(fatura);
    }

    private IadeDTO entityToDTO(Iade i) {
        List<IadeKalem> kalemEntities = iadeKalemRepository.findByIadeId(i.getId());

        List<Long> stokIdler = kalemEntities.stream()
                .map(IadeKalem::getStokId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Stok> stokHaritasi = stokIdler.isEmpty()
                ? Map.of()
                : stokRepository.findAllById(stokIdler).stream()
                        .collect(Collectors.toMap(Stok::getId, s -> s));

        List<IadeKalemDTO> kalemler = kalemEntities.stream()
                .map(k -> {
                    String stokAd = null;
                    String stokKodu = null;
                    if (k.getStokId() != null) {
                        Stok stok = stokHaritasi.get(k.getStokId());
                        if (stok != null) {
                            stokAd = stok.getAd();
                            stokKodu = stok.getStokKodu();
                        }
                    }
                    return IadeKalemDTO.builder()
                            .id(k.getId()).stokId(k.getStokId())
                            .stokAd(stokAd).stokKodu(stokKodu)
                            .aciklama(k.getAciklama()).miktar(k.getMiktar())
                            .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                            .kdvOrani(k.getKdvOrani()).tutar(k.getTutar())
                            .build();
                }).collect(Collectors.toList());

        return IadeDTO.builder()
                .id(i.getId()).faturaId(i.getFaturaId()).tur(i.getTur()).tarih(i.getTarih())
                .tutar(i.getTutar()).aciklama(i.getAciklama()).durum(i.getDurum())
                .sirketId(i.getSirketId()).olusturmaTarihi(i.getOlusturmaTarihi())
                .kalemler(kalemler)
                .build();
    }
}
