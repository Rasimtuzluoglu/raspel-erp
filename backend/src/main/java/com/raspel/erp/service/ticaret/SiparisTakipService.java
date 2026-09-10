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
        return siparisler.stream().map(s -> {
            List<UretimEmri> emirler = uretimEmriRepository.findBySirketIdAndSiparisId(sirketId, s.getId());
            List<Irsaliye> irsaliyeler = irsaliyeRepository.findBySirketIdAndSiparisId(sirketId, s.getId());
            List<Teslimat> teslimatlar = teslimatRepository.findBySirketIdAndSiparisId(sirketId, s.getId());

            String cariAd = s.getCariHesapId() != null
                    ? cariHesapRepository.findById(s.getCariHesapId()).map(c -> c.getAd()).orElse(null)
                    : null;

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
                    .build();
        }).collect(Collectors.toList());
    }
}
