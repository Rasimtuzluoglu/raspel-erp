package com.raspel.erp.service.ticaret;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP tabanlı GİB/entegratör adaptörü. UBL-TR XML'i entegratör uç noktasına POST eder
 * ve durum sorgusunu GET ile yapar. Uç nokta tanımlı değilse gönderim/sorgulama yapılmaz
 * (sahte onay üretilmez). Farklı entegratörler için EEntegratorAdaptoru uygulanabilir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HttpEEntegratorAdaptoru implements EEntegratorAdaptoru {

    private final RestTemplate restTemplate;

    @Value("${app.entegrator.url:${app.efatura.gib-endpoint:}}")
    private String entegratorUrl;

    @Value("${app.entegrator.api-key:}")
    private String entegratorApiKey;

    @Override
    public boolean tanimliMi() {
        return entegratorUrl != null && !entegratorUrl.isBlank();
    }

    @Override
    public boolean gonder(String ettn, String belgeTuru, String ublXml) {
        if (!tanimliMi()) return false;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            if (entegratorApiKey != null && !entegratorApiKey.isBlank()) {
                headers.set("Authorization", "Bearer " + entegratorApiKey);
            }
            headers.set("X-Belge-Turu", belgeTuru != null ? belgeTuru : "EFATURA");
            restTemplate.postForEntity(entegratorUrl, new HttpEntity<>(ublXml, headers), String.class);
            log.info("E-belge entegratöre iletildi - ETTN: {}, Tür: {}", ettn, belgeTuru);
            return true;
        } catch (Exception e) {
            log.warn("E-belge entegratöre gönderilemedi ({}): {}", ettn, e.getMessage());
            return false;
        }
    }

    @Override
    public Integer durumSorgula(String ettn) {
        if (!tanimliMi()) return null;
        try {
            String sorguUrl = entegratorUrl.endsWith("/")
                    ? entegratorUrl + ettn + "/durum"
                    : entegratorUrl + "/" + ettn + "/durum";
            HttpHeaders headers = new HttpHeaders();
            if (entegratorApiKey != null && !entegratorApiKey.isBlank()) {
                headers.set("Authorization", "Bearer " + entegratorApiKey);
            }
            var yanit = restTemplate.exchange(sorguUrl, org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers), java.util.Map.class);
            if (yanit.getBody() != null && yanit.getBody().get("durumKodu") instanceof Number n) {
                return n.intValue();
            }
        } catch (Exception e) {
            log.warn("Entegratör durum sorgusu başarısız ({}): {}", ettn, e.getMessage());
        }
        return null;
    }
}
