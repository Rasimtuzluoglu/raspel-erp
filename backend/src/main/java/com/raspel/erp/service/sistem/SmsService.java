package com.raspel.erp.service.sistem;

import com.raspel.erp.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SMS gönderim adaptörü. Sağlayıcı bilgileri (app.sms.url, app.sms.api-key, app.sms.gonderen)
 * tanımlıysa HTTP üzerinden gönderir; tanımlı değilse açık hata döner (sahte başarı üretilmez).
 * Canlı gönderim müşterinin kendi sağlayıcı bilgileriyle yapılır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final RestTemplate restTemplate;

    @Value("${app.sms.url:}")
    private String smsUrl;

    @Value("${app.sms.api-key:}")
    private String smsApiKey;

    @Value("${app.sms.gonderen:}")
    private String smsGonderen;

    public boolean saglayiciTanimliMi() {
        return smsUrl != null && !smsUrl.isBlank();
    }

    /** SMS gönderir. Sağlayıcı tanımlı değilse BusinessException fırlatır. */
    public void gonder(String telefon, String mesaj) {
        if (telefon == null || telefon.isBlank()) {
            throw new BusinessException("Telefon numarası zorunludur");
        }
        if (mesaj == null || mesaj.isBlank()) {
            throw new BusinessException("Mesaj içeriği zorunludur");
        }
        if (!saglayiciTanimliMi()) {
            throw new BusinessException("SMS sağlayıcı tanımlı değil (app.sms.url). "
                    + "Canlı gönderim için sağlayıcı bilgilerini yapılandırın.");
        }
        Map<String, Object> govde = new LinkedHashMap<>();
        govde.put("telefon", telefon);
        govde.put("mesaj", mesaj);
        if (smsGonderen != null && !smsGonderen.isBlank()) {
            govde.put("gonderen", smsGonderen);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (smsApiKey != null && !smsApiKey.isBlank()) {
            headers.set("Authorization", "Bearer " + smsApiKey);
        }
        try {
            restTemplate.postForEntity(smsUrl, new HttpEntity<>(govde, headers), String.class);
            log.info("SMS gönderildi: {}", telefon);
        } catch (Exception e) {
            log.warn("SMS gönderilemedi ({}): {}", telefon, e.getMessage());
            throw new BusinessException("SMS gönderilemedi: " + e.getMessage());
        }
    }
}
