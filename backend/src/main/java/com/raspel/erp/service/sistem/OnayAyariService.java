package com.raspel.erp.service.sistem;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sistem.OnayAyariDTO;
import com.raspel.erp.entity.sistem.OnayAyari;
import com.raspel.erp.repository.sistem.OnayAyariRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OnayAyariService {

    private final OnayAyariRepository onayAyariRepository;
    private final TenantChecker tenantChecker;

    private static final List<String> MODULLER = List.of("MASRAF", "SATINALMA", "IZIN");

    public List<OnayAyariDTO> listele(Long sirketId) {
        Map<String, OnayAyari> kayitli = onayAyariRepository.findBySirketIdOrderByModulAsc(sirketId)
                .stream().collect(Collectors.toMap(OnayAyari::getModul, a -> a));

        // Kayıtlı olmayan modüller varsayılan değerlerle döner, böylece UI her zaman dolu görünür.
        return MODULLER.stream().map(m -> {
            OnayAyari a = kayitli.get(m);
            if (a != null) return entityToDTO(a);
            return OnayAyariDTO.builder()
                    .sirketId(sirketId).modul(m)
                    .esikTutar(BigDecimal.ZERO).otomatikOnay(false)
                    .build();
        }).collect(Collectors.toList());
    }

    public OnayAyariDTO kaydet(Long sirketId, OnayAyariDTO dto) {
        String modul = dto.getModul() != null ? dto.getModul().toUpperCase() : null;
        if (modul == null || !MODULLER.contains(modul)) {
            throw new com.raspel.erp.exception.BusinessException("Geçersiz modül: " + dto.getModul());
        }
        OnayAyari ayar = onayAyariRepository.findBySirketIdAndModul(sirketId, modul)
                .orElseGet(() -> OnayAyari.builder().sirketId(sirketId).modul(modul)
                        .esikTutar(BigDecimal.ZERO).otomatikOnay(false).build());

        ayar.setEsikTutar(dto.getEsikTutar() != null ? dto.getEsikTutar() : BigDecimal.ZERO);
        ayar.setOtomatikOnay(dto.getOtomatikOnay() != null && dto.getOtomatikOnay());
        return entityToDTO(onayAyariRepository.save(ayar));
    }

    /** Eşiğin altındaki tutar otomatik onaya tabi mi? */
    @Transactional(readOnly = true)
    public boolean otomatikOnayGecerli(Long sirketId, String modul, BigDecimal tutar) {
        return onayAyariRepository.findBySirketIdAndModul(sirketId, modul != null ? modul.toUpperCase() : null)
                .map(a -> Boolean.TRUE.equals(a.getOtomatikOnay())
                        && tutar != null
                        && tutar.compareTo(a.getEsikTutar()) <= 0)
                .orElse(false);
    }

    private OnayAyariDTO entityToDTO(OnayAyari a) {
        return OnayAyariDTO.builder()
                .id(a.getId()).sirketId(a.getSirketId()).modul(a.getModul())
                .esikTutar(a.getEsikTutar()).otomatikOnay(a.getOtomatikOnay())
                .build();
    }
}
