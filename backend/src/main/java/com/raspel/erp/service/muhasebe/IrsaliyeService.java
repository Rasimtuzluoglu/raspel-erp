package com.raspel.erp.service.muhasebe;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.muhasebe.IrsaliyeDTO;
import com.raspel.erp.dto.muhasebe.IrsaliyeKalemDTO;
import com.raspel.erp.entity.muhasebe.Irsaliye;
import com.raspel.erp.entity.muhasebe.IrsaliyeKalem;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeKalemRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class IrsaliyeService {

    private final IrsaliyeRepository irsaliyeRepository;
    private final IrsaliyeKalemRepository kalemRepository;
    private final CariHesapRepository cariHesapRepository;
    private final StokRepository stokRepository;
    private final StokHareketRepository stokHareketRepository;
    private final TenantChecker tenantChecker;
    private final CacheYardimci cacheYardimci;
    private final com.raspel.erp.service.sube.DepoStokService depoStokService;
    private final com.raspel.erp.service.envanter.StokSeriService stokSeriService;
    private final com.raspel.erp.service.envanter.MaliyetService maliyetService;
    private final com.raspel.erp.service.sistem.DonemService donemService;
    private final com.raspel.erp.repository.ticaret.FaturaRepository faturaRepository;
    private final com.raspel.erp.service.ticaret.FaturaService faturaService;

    @Transactional(readOnly = true)
    public Page<IrsaliyeDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return irsaliyeRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public IrsaliyeDTO getir(Long id) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");
        return entityToDTO(i);
    }

    public IrsaliyeDTO olustur(IrsaliyeDTO dto, Long sirketId) {
        donemService.kilitKontrol(sirketId,
                dto.getTarih() != null ? dto.getTarih() : LocalDate.now(), "irsaliye oluşturma");
        Irsaliye i = Irsaliye.builder()
                .irsaliyeNo(dto.getIrsaliyeNo()).tarih(dto.getTarih())
                .cariHesapId(dto.getCariHesapId()).faturaId(dto.getFaturaId())
                .siparisId(dto.getSiparisId())
                .durum("TASLAK").tur(dto.getTur() != null ? dto.getTur() : "SATIS")
                .aciklama(dto.getAciklama()).sirketId(sirketId).depoId(dto.getDepoId()).build();
        i = irsaliyeRepository.save(i);
        if (dto.getKalemler() != null) {
            for (IrsaliyeKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(IrsaliyeKalem.builder()
                        .irsaliyeId(i.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).build());
            }
        }
        return entityToDTO(i);
    }

    /**
     * Kesilmiş irsaliyeyi faturaya dönüştürür. İrsaliye stoğu zaten işlediği için
     * faturaya {@code irsaliyeId} bağlanır; böylece fatura kesilirken stok tekrar
     * düşülmez (çift düşüm önlenir). İrsaliyeye oluşan fatura bağlanır.
     */
    @Transactional
    public IrsaliyeDTO faturayaDonustur(Long id, Long sirketId) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");
        if (!"KESILDI".equals(i.getDurum())) {
            throw new BusinessException("Yalnızca kesilmiş irsaliye faturaya dönüştürülebilir.");
        }
        if (i.getFaturaId() != null) {
            var mevcut = faturaRepository.findById(i.getFaturaId()).orElse(null);
            if (mevcut != null && mevcut.getDurum() != Fatura.FaturaDurum.IPTAL) {
                throw new BusinessException("Bu irsaliye zaten faturaya dönüştürülmüş: " + mevcut.getFaturaNumarasi());
            }
        }
        List<IrsaliyeKalem> kalemler = kalemRepository.findByIrsaliyeId(i.getId());
        List<com.raspel.erp.dto.ticaret.FaturaKalemDTO> faturaKalemler = new java.util.ArrayList<>();
        for (IrsaliyeKalem k : kalemler) {
            // İrsaliye kaleminde fiyat/KDV yok; stok kartından satış fiyatı ve KDV oranı çözülür.
            java.math.BigDecimal birimFiyat = java.math.BigDecimal.ZERO;
            java.math.BigDecimal kdv = new java.math.BigDecimal("20");
            if (k.getStokId() != null) {
                var stok = stokRepository.findById(k.getStokId()).orElse(null);
                if (stok != null) {
                    birimFiyat = stok.getSatisFiyati() != null ? stok.getSatisFiyati()
                            : (stok.getFiyat() != null ? stok.getFiyat() : java.math.BigDecimal.ZERO);
                    if (stok.getKdvOrani() != null) kdv = stok.getKdvOrani();
                }
            }
            faturaKalemler.add(com.raspel.erp.dto.ticaret.FaturaKalemDTO.builder()
                    .aciklama(k.getAciklama() != null ? k.getAciklama() : "")
                    .adet(k.getMiktar() != null ? k.getMiktar() : java.math.BigDecimal.ONE)
                    .birimFiyat(birimFiyat).kdvOrani(kdv).stokId(k.getStokId()).build());
        }
        com.raspel.erp.dto.ticaret.FaturaDTO faturaDTO = com.raspel.erp.dto.ticaret.FaturaDTO.builder()
                .tarih(java.time.LocalDate.now())
                .tur("SATIS").durum("KESILDI")
                .cariHesapId(i.getCariHesapId())
                .irsaliyeId(i.getId())
                .siparisId(i.getSiparisId())
                .depoId(i.getDepoId())
                .aciklama("İrsaliye #" + i.getIrsaliyeNo() + " dönüşümü")
                .kalemler(faturaKalemler)
                .build();
        var olusan = faturaService.faturaOlustur(faturaDTO, i.getSirketId(), null, null);
        i.setFaturaId(olusan.getId());
        irsaliyeRepository.save(i);
        cacheYardimci.temizle("dashboard");
        return entityToDTO(i);
    }

    public IrsaliyeDTO guncelle(Long id, IrsaliyeDTO dto) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");
        donemService.kilitKontrol(i.getSirketId(), i.getTarih(), "irsaliye düzenleme");
        if (dto.getTarih() != null) {
            donemService.kilitKontrol(i.getSirketId(), dto.getTarih(), "irsaliye düzenleme");
        }
        i.setIrsaliyeNo(dto.getIrsaliyeNo());
        i.setTarih(dto.getTarih());
        i.setCariHesapId(dto.getCariHesapId());
        i.setFaturaId(dto.getFaturaId());
        if (dto.getDurum() != null) i.setDurum(dto.getDurum());
        if (dto.getTur() != null) i.setTur(dto.getTur());
        if (dto.getDepoId() != null) i.setDepoId(dto.getDepoId());
        i.setAciklama(dto.getAciklama());
        i = irsaliyeRepository.save(i);
        if (dto.getKalemler() != null) {
            kalemRepository.deleteByIrsaliyeId(i.getId());
            for (IrsaliyeKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(IrsaliyeKalem.builder()
                        .irsaliyeId(i.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).build());
            }
        }
        return entityToDTO(i);
    }

    public IrsaliyeDTO durumGuncelle(Long id, String durum) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");
        donemService.kilitKontrol(i.getSirketId(), i.getTarih(), "irsaliye durum güncelleme");

        if ("KESILDI".equals(durum) && !"KESILDI".equals(i.getDurum())) {
            List<IrsaliyeKalem> kalemler = kalemRepository.findByIrsaliyeId(i.getId());
            Long depoId = depoStokService.coz(i.getDepoId(), i.getSirketId());
            for (IrsaliyeKalem k : kalemler) {
                if (k.getStokId() == null) continue;
                Stok stok = stokRepository.findByIdForUpdate(k.getStokId())
                        .orElseThrow(() -> new ResourceNotFoundException("Stok", k.getStokId()));
                BigDecimal adet = k.getMiktar() != null ? k.getMiktar() : BigDecimal.ZERO;
                if ("SATIS".equals(i.getTur()) && stok.getMiktar().compareTo(adet) < 0) {
                    throw new BusinessException("Yetersiz stok! Ürün: " + stok.getAd()
                            + ", Mevcut: " + stok.getMiktar() + ", İstenen: " + adet);
                }
                if ("SATIS".equals(i.getTur())) {
                    stok.setMiktar(stok.getMiktar().subtract(adet));
                    maliyetService.cikisIsle(stok, adet, stok.getMiktar(), i.getSirketId(), "IRSALIYE", i.getId());
                } else {
                    BigDecimal eskiMiktar = stok.getMiktar() != null ? stok.getMiktar() : BigDecimal.ZERO;
                    stok.setMiktar(stok.getMiktar().add(adet));
                    maliyetService.girisIsle(stok, eskiMiktar, adet, null, i.getSirketId(), "IRSALIYE", i.getId());
                }
                stokRepository.save(stok);
                Long seriId = null;
                if ("SATIS".equals(i.getTur())) {
                    var seriler = stokSeriService.fefoTuket(stok.getId(), depoId, adet);
                    if (seriler.size() == 1) seriId = seriler.get(0);
                }
                stokHareketRepository.save(StokHareket.builder()
                        .stok(stok)
                        .tur("SATIS".equals(i.getTur()) ? "CIKIS" : "GIRIS")
                        .miktar(adet)
                        .hareketTarihi(i.getTarih() != null ? i.getTarih() : LocalDate.now())
                        .aciklama("İrsaliye #" + i.getIrsaliyeNo())
                        .depoId(depoId)
                        .seriId(seriId)
                        .kaynakTip("IRSALIYE").kaynakId(i.getId())
                        .build());
                depoStokService.guncelle(depoId, stok.getId(),
                        "SATIS".equals(i.getTur()) ? adet.negate() : adet);
            }
        } else if (("IPTAL".equals(durum) || "TASLAK".equals(durum)) && "KESILDI".equals(i.getDurum())) {
            // Bağlı (iptal olmayan) fatura varsa irsaliye geri alınamaz; aksi halde stok geri
            // eklenirken fatura kesilmiş kalır ve çift/eksik stok oluşur.
            if (i.getFaturaId() != null) {
                var bagliFatura = faturaRepository.findById(i.getFaturaId()).orElse(null);
                if (bagliFatura != null && bagliFatura.getDurum() != Fatura.FaturaDurum.IPTAL) {
                    throw new BusinessException(
                            "Bu irsaliyeye bağlı kesilmiş fatura var. Önce faturayı iptal edin.");
                }
            }
            // Kesilmis irsaliyeden geri donus (TASLAK/IPTAL): stok etkisi geri alinir.
            String sebep = "IPTAL".equals(durum) ? "İrsaliye iptal" : "İrsaliye geri alındı";
            List<IrsaliyeKalem> kalemler = kalemRepository.findByIrsaliyeId(i.getId());
            Long depoId = depoStokService.coz(i.getDepoId(), i.getSirketId());
            for (IrsaliyeKalem k : kalemler) {
                if (k.getStokId() == null) continue;
                Stok stok = stokRepository.findByIdForUpdate(k.getStokId())
                        .orElseThrow(() -> new ResourceNotFoundException("Stok", k.getStokId()));
                BigDecimal miktar = k.getMiktar() != null ? k.getMiktar() : BigDecimal.ZERO;
                if ("SATIS".equals(i.getTur())) {
                    BigDecimal eskiMiktar = stok.getMiktar() != null ? stok.getMiktar() : BigDecimal.ZERO;
                    stok.setMiktar(stok.getMiktar().add(miktar));
                    maliyetService.girisIsle(stok, eskiMiktar, miktar, null, i.getSirketId(), "IRSALIYE", i.getId());
                } else {
                    stok.setMiktar(stok.getMiktar().subtract(miktar));
                    maliyetService.cikisIsle(stok, miktar, stok.getMiktar(), i.getSirketId(), "IRSALIYE", i.getId());
                }
                stokRepository.save(stok);
                stokHareketRepository.save(StokHareket.builder()
                        .stok(stok)
                        .tur("SATIS".equals(i.getTur()) ? "GIRIS" : "CIKIS")
                        .miktar(miktar)
                        .hareketTarihi(i.getTarih() != null ? i.getTarih() : LocalDate.now())
                        .aciklama(sebep + " #" + i.getIrsaliyeNo())
                        .depoId(depoId)
                        .kaynakTip("IRSALIYE").kaynakId(i.getId())
                        .build());
                depoStokService.guncelle(depoId, stok.getId(),
                        "SATIS".equals(i.getTur()) ? miktar : miktar.negate());
            }
        }

        if ("KESILDI".equals(durum) || "IPTAL".equals(durum)) {
            cacheYardimci.temizle("stoklar", "dashboard");
        }

        i.setDurum(durum);
        return entityToDTO(irsaliyeRepository.save(i));
    }

    public void sil(Long id) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");
        donemService.kilitKontrol(i.getSirketId(), i.getTarih(), "irsaliye silme");
        if ("KESILDI".equals(i.getDurum())) {
            throw new BusinessException("Kesilmiş irsaliye doğrudan silinemez, önce iptal edilmelidir");
        }
        kalemRepository.deleteByIrsaliyeId(id);
        irsaliyeRepository.deleteById(id);
    }

    private IrsaliyeDTO entityToDTO(Irsaliye i) {
        List<IrsaliyeKalem> kalemEntities = kalemRepository.findByIrsaliyeId(i.getId());

        List<Long> stokIdler = kalemEntities.stream()
                .map(IrsaliyeKalem::getStokId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> stokAdlari = stokIdler.isEmpty() ? Map.of()
                : stokRepository.findAllById(stokIdler).stream()
                        .collect(Collectors.toMap(Stok::getId, Stok::getAd));

        String cariAdi = i.getCariHesapId() != null
                ? cariHesapRepository.findById(i.getCariHesapId()).map(c -> c.getAd()).orElse(null)
                : null;

        List<IrsaliyeKalemDTO> kalemler = kalemEntities.stream()
                .map(k -> IrsaliyeKalemDTO.builder().id(k.getId())
                        .stokId(k.getStokId())
                        .stokAdi(k.getStokId() != null ? stokAdlari.get(k.getStokId()) : null)
                        .aciklama(k.getAciklama()).miktar(k.getMiktar()).birim(k.getBirim()).build())
                .collect(Collectors.toList());
        return IrsaliyeDTO.builder().id(i.getId()).irsaliyeNo(i.getIrsaliyeNo()).tarih(i.getTarih())
                .cariHesapId(i.getCariHesapId())
                .cariHesapAdi(cariAdi)
                .faturaId(i.getFaturaId()).durum(i.getDurum()).tur(i.getTur())
                .aciklama(i.getAciklama()).sirketId(i.getSirketId()).depoId(i.getDepoId())
                .olusturmaTarihi(i.getOlusturmaTarihi()).kalemler(kalemler).build();
    }
}