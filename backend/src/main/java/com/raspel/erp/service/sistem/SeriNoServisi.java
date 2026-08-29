package com.raspel.erp.service.sistem;

import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.repository.ticaret.TeklifRepository;
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
    private final TeklifRepository teklifRepository;

    // NOT: `synchronized` yalnızca tek JVM instance içinde mükerrer üretimi engeller.
    // Çoklu instance (yatay ölçekleme) senaryosunda asıl koruma, tablo üzerindeki
    // UNIQUE kısıtlardır (fatura.fatura_numarasi, siparis.siparis_no, ticaret.teklif_no).
    // Böylece iki instance aynı numarayı üretmeye çalışsa bile DB ikincisini reddeder.

    public synchronized String faturaNoUret(Long sirketId) {
        String yil = String.valueOf(LocalDate.now().getYear());
        String prefix = "FTR-" + (sirketId != null ? sirketId + "-" : "") + yil + "-";
        List<String> mevcutlar = faturaRepository.findFaturaNumarasiByPrefix(prefix, sirketId);
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

    public synchronized String siparisNoUret(Long sirketId) {
        String yil = String.valueOf(LocalDate.now().getYear());
        String prefix = "SIP-" + (sirketId != null ? sirketId + "-" : "") + yil + "-";
        List<String> mevcutlar = siparisRepository.findSiparisNoByPrefix(prefix, sirketId);
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

    public synchronized String teklifNoUret(Long sirketId) {
        String yil = String.valueOf(LocalDate.now().getYear());
        String prefix = "TKL-" + (sirketId != null ? sirketId + "-" : "") + yil + "-";
        List<String> mevcutlar = teklifRepository.findTeklifNoByPrefix(prefix, sirketId);
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
