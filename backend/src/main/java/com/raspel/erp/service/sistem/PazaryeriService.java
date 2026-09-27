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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pazaryeri (marketplace) adaptör iskeleti. Stok/fiyat güncellemesini yapılandırılmış
 * uç noktaya (app.pazaryeri.url) JSON olarak gönderir. Uç nokta tanımlı değilse açık
 * hata döner; böylece canlı entegrasyon müşteri kimliğiyle devreye alınır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PazaryeriService {

    private final RestTemplate restTemplate;

    @Value("${app.pazaryeri.url:}")
    private String pazaryeriUrl;

    @Value("${app.pazaryeri.api-key:}")
    private String pazaryeriApiKey;

    public boolean saglayiciTanimliMi() {
        return pazaryeriUrl != null && !pazaryeriUrl.isBlank();
    }

    /** Stok miktarı ve fiyatını pazaryerine bildirir. */
    public void stokGonder(Long stokId, String stokKodu, BigDecimal miktar, BigDecimal fiyat) {
        if (!saglayiciTanimliMi()) {
            throw new BusinessException("Pazaryeri uç noktası tanımlı değil (app.pazaryeri.url). "
                    + "Canlı entegrasyon için sağlayıcı bilgilerini yapılandırın.");
        }
        Map<String, Object> govde = new LinkedHashMap<>();
        govde.put("stokId", stokId);
        govde.put("stokKodu", stokKodu);
        govde.put("miktar", miktar);
        govde.put("fiyat", fiyat);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (pazaryeriApiKey != null && !pazaryeriApiKey.isBlank()) {
            headers.set("Authorization", "Bearer " + pazaryeriApiKey);
        }
        try {
            restTemplate.postForEntity(pazaryeriUrl, new HttpEntity<>(govde, headers), String.class);
            log.info("Pazaryeri stok güncellemesi gönderildi - Stok: {}", stokKodu);
        } catch (Exception e) {
            log.warn("Pazaryeri gönderimi başarısız (stok {}): {}", stokKodu, e.getMessage());
            throw new BusinessException("Pazaryeri gönderimi başarısız: " + e.getMessage());
        }
    }
}
