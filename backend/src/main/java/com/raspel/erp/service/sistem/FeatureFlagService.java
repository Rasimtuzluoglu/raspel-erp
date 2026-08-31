package com.raspel.erp.service.sistem;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Basit feature flag sistemi. Yeni özellikleri kademeli açmak için
 * application.properties üzerinden `app.features.*` anahtarlarıyla kontrol edilir.
 * Varsayılan olarak tüm bilinen flag'ler AÇIK değildir (fail-closed değil; bilinen
 * flag'ler varsayılan olarak açık, bilinmeyenler false döner).
 */
@Service
@Slf4j
public class FeatureFlagService {

    @Value("${app.features.enabled:}")
    private String enabledList;

    @Value("${app.features.disabled:}")
    private String disabledList;

    public boolean aktif(String flag) {
        if (flag == null || flag.isBlank()) return false;
        Set<String> enabled = parse(enabledList);
        Set<String> disabled = parse(disabledList);
        if (disabled.contains(flag)) return false;
        if (enabled.contains(flag)) return true;
        // Bilinen güvenli varsayılanlar açık; diğerleri kapalı.
        return Set.of("churn-analizi", "api-token", "butce-pdf", "efatura-durum-sorgu").contains(flag);
    }

    public Map<String, Boolean> tumu() {
        Map<String, Boolean> sonuc = new HashMap<>();
        for (String f : Set.of("churn-analizi", "api-token", "butce-pdf", "efatura-durum-sorgu")) {
            sonuc.put(f, aktif(f));
        }
        parse(enabledList).forEach(f -> sonuc.put(f, true));
        parse(disabledList).forEach(f -> sonuc.put(f, false));
        return sonuc;
    }

    private Set<String> parse(String liste) {
        if (liste == null || liste.isBlank()) return Set.of();
        return Arrays.stream(liste.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }
}
