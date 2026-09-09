package com.raspel.erp.service.sistem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GitHub üzerinden güncelleme kontrolü. Çalışan sürüm ile GitHub'daki son
 * sürüm/commit bilgisini karşılaştırır. (Gerçek uygulama sunucu tarafında
 * `git pull && docker compose up -d --build` ile yapılır.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuncellemeService {

    private final RestTemplate restTemplate;

    @Value("${app.version:1.8.0}")
    private String surum;

    @Value("${app.github.repo:Rasimtuzluoglu/raspel-erp}")
    private String repo;

    @Value("${app.github.branch:main}")
    private String branch;

    public Map<String, Object> durum() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("mevcutSurum", surum);
        try {
            // Önce son sürüm (release) dene
            try {
                Map<?, ?> rel = restTemplate.getForObject(
                        "https://api.github.com/repos/" + repo + "/releases/latest", Map.class);
                if (rel != null && rel.get("tag_name") != null) {
                    String sonSurum = String.valueOf(rel.get("tag_name"));
                    r.put("sonSurum", sonSurum);
                    r.put("guncellemeVar", !esitSurum(surum, sonSurum));
                }
            } catch (Exception e) {
                r.put("sonSurum", null);
            }

            // Son commit bilgisini getir
            List<?> commits = restTemplate.getForObject(
                    "https://api.github.com/repos/" + repo + "/commits?per_page=1&sha=" + branch, List.class);
            if (commits != null && !commits.isEmpty() && commits.get(0) instanceof Map<?, ?> c) {
                String sha = String.valueOf(c.get("sha"));
                r.put("sonCommit", sha.length() > 7 ? sha.substring(0, 7) : sha);
                Map<?, ?> commit = (Map<?, ?>) c.get("commit");
                if (commit != null) {
                    r.put("sonCommitMesaj", commit.get("message"));
                    Map<?, ?> yazar = (Map<?, ?>) commit.get("author");
                    if (yazar != null) r.put("sonCommitTarih", yazar.get("date"));
                }
            }
        } catch (Exception e) {
            log.warn("GitHub güncelleme kontrolü başarısız: {}", e.getMessage());
            r.put("hata", "GitHub'a ulaşılamadı: " + e.getMessage());
        }
        return r;
    }

    private boolean esitSurum(String mevcut, String son) {
        String a = mevcut != null ? mevcut.trim().toLowerCase() : "";
        String b = son != null ? son.trim().toLowerCase() : "";
        return a.equals(b) || ("v" + a).equals(b);
    }
}
