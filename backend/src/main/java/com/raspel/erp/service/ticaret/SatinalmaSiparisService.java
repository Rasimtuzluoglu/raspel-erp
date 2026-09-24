package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.FaturaDTO;
import com.raspel.erp.dto.ticaret.FaturaKalemDTO;
import com.raspel.erp.dto.ticaret.SatinalmaSiparisDTO;
import com.raspel.erp.dto.ticaret.SatinalmaSiparisKalemDTO;
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
        SatinalmaSiparis s = SatinalmaSiparis.builder()
                .siparisNo(dto.getSiparisNo())
                .tarih(dto.getTarih())
                .cariHesapId(dto.getCariHesapId())
                .talepId(dto.getTalepId())
                .durum("TASLAK")
                .aciklama(dto.getAciklama())
                .araToplam(dto.getAraToplam())
                .kdv(dto.getKdv())
                .genelToplam(dto.getGenelToplam())
                .sirketId(dto.getSirketId())
                .build();
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
        return entityToDTO(s);
    }

    public SatinalmaSiparisDTO guncelle(Long id, SatinalmaSiparisDTO dto) {
        SatinalmaSiparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        if ("FATURALANDI".equals(s.getDurum())) {
            throw new BusinessException("Faturası oluşturulmuş satınalma siparişi doğrudan düzenlenemez");
        }
        s.setSiparisNo(dto.getSiparisNo());
        s.setTarih(dto.getTarih());
        s.setCariHesapId(dto.getCariHesapId());
        s.setTalepId(dto.getTalepId());
        if (dto.getDurum() != null) s.setDurum(dto.getDurum());
        s.setAciklama(dto.getAciklama());
        s.setAraToplam(dto.getAraToplam());
        s.setKdv(dto.getKdv());
        s.setGenelToplam(dto.getGenelToplam());
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

    public SatinalmaSiparisDTO durumGuncelle(Long id, String durum) {
        SatinalmaSiparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        s.setDurum(durum);
        return entityToDTO(siparisRepository.save(s));
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
                .olusturmaTarihi(s.getOlusturmaTarihi()).kalemler(kalemlerDto).build();
    }
}