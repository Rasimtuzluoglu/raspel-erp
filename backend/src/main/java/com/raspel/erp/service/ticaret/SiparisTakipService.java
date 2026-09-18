package com.raspel.erp.service.ticaret;

import com.raspel.erp.dto.ticaret.SiparisTakipDTO;
import com.raspel.erp.entity.envanter.UretimEmri;
import com.raspel.erp.entity.muhasebe.Irsaliye;
import com.raspel.erp.entity.ticaret.Siparis;
import com.raspel.erp.entity.ticaret.Teslimat;
import com.raspel.erp.repository.envanter.UretimEmriRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.repository.ticaret.TeslimatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sipariş -> Üretim -> Sevk -> Teslimat zincirini okur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SiparisTakipService {

    private final SiparisRepository siparisRepository;
    private final UretimEmriRepository uretimEmriRepository;
    private final IrsaliyeRepository irsaliyeRepository;
    private final TeslimatRepository teslimatRepository;
    private final CariHesapRepository cariHesapRepository;

    @Transactional(readOnly = true)
    public List<SiparisTakipDTO> zincir(Long sirketId) {
        List<Siparis> siparisler = siparisRepository.findBySirketIdOrderByTarihDesc(sirketId, PageRequest.of(0, 200)).getContent();
        if (siparisler.isEmpty()) return List.of();

        List<Long> siparisIdler = siparisler.stream().map(Siparis::getId).collect(Collectors.toList());

        // Zincir verileri sipariş başına ayrı sorgu yerine toplu çekilir (N+1 önlenir).
        Map<Long, List<UretimEmri>> emirMap = uretimEmriRepository
                .findBySirketIdAndSiparisIdIn(sirketId, siparisIdler).stream()
                .collect(Collectors.groupingBy(UretimEmri::getSiparisId));
        Map<Long, List<Irsaliye>> irsaliyeMap = irsaliyeRepository
                .findBySirketIdAndSiparisIdIn(sirketId, siparisIdler).stream()
                .collect(Collectors.groupingBy(Irsaliye::getSiparisId));
        Map<Long, List<Teslimat>> teslimatMap = teslimatRepository
                .findBySirketIdAndSiparisIdIn(sirketId, siparisIdler).stream()
                .collect(Collectors.groupingBy(Teslimat::getSiparisId));

        java.util.Set<Long> cariIdler = siparisler.stream().map(Siparis::getCariHesapId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> cariAdlari = cariIdler.isEmpty() ? Map.of()
                : cariHesapRepository.findAllById(cariIdler).stream()
                        .collect(Collectors.toMap(com.raspel.erp.entity.finans.CariHesap::getId,
                                com.raspel.erp.entity.finans.CariHesap::getAd, (a, b) -> a));

        return siparisler.stream().map(s -> {
            List<UretimEmri> emirler = emirMap.getOrDefault(s.getId(), List.of());
            List<Irsaliye> irsaliyeler = irsaliyeMap.getOrDefault(s.getId(), List.of());
            List<Teslimat> teslimatlar = teslimatMap.getOrDefault(s.getId(), List.of());

            String cariAd = s.getCariHesapId() != null ? cariAdlari.get(s.getCariHesapId()) : null;

            Teslimat t = teslimatlar.isEmpty() ? null : teslimatlar.get(0);
            boolean teslimatGecikti = t != null && t.getBeklenenTeslimTarihi() != null
                    && t.getBeklenenTeslimTarihi().isBefore(java.time.LocalDate.now())
                    && !Teslimat.Durum.TESLIM_EDILDI.name().equals(t.getDurum())
                    && !Teslimat.Durum.IPTAL.name().equals(t.getDurum());

            return SiparisTakipDTO.builder()
                    .siparisId(s.getId())
                    .siparisNo(s.getSiparisNo())
                    .cariAd(cariAd)
                    .siparisDurum(s.getDurum())
                    .driverAd(s.getDriverAd())
                    .uretimDurum(emirler.isEmpty() ? null : emirler.get(0).getDurum())
                    .uretimSayisi(emirler.size())
                    .sevkDurum(irsaliyeler.isEmpty() ? null : irsaliyeler.get(0).getDurum())
                    .sevkSayisi(irsaliyeler.size())
                    .teslimatDurum(teslimatlar.isEmpty() ? null : teslimatlar.get(0).getDurum())
                    .teslimatSayisi(teslimatlar.size())
                    .beklenenTeslimTarihi(t != null ? t.getBeklenenTeslimTarihi() : null)
                    .teslimatGecikti(teslimatGecikti)
                    .build();
        }).collect(Collectors.toList());
    }
}
