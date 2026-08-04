package com.raspel.erp.service.sistem;

import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeriNoServisi {

    private final FaturaRepository faturaRepository;
    private final SiparisRepository siparisRepository;

    public String faturaNoUret() {
        String yil = String.valueOf(LocalDate.now().getYear());
        String prefix = "FTR-" + yil + "-";
        List<String> mevcutlar = faturaRepository.findFaturaNumarasiByPrefix(prefix);
        int maxSeri = mevcutlar.stream()
                .mapToInt(no -> {
                    try {
                        return Integer.parseInt(no.substring(prefix.length()));
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return prefix + String.format("%06d", maxSeri + 1);
    }

    public String siparisNoUret() {
        String yil = String.valueOf(LocalDate.now().getYear());
        String prefix = "SIP-" + yil + "-";
        List<String> mevcutlar = siparisRepository.findSiparisNoByPrefix(prefix);
        int maxSeri = mevcutlar.stream()
                .mapToInt(no -> {
                    try {
                        return Integer.parseInt(no.substring(prefix.length()));
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return prefix + String.format("%06d", maxSeri + 1);
    }
}
