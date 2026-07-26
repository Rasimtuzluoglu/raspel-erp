package com.raspel.erp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.util.Map;

@Configuration
@Slf4j
public class InfrastructureStubs {

    /**
     * 1. Fatura & Fiş OCR Servis Altyapı Taslağı
     */
    @Service
    @Slf4j
    public static class FaturaOcrStubService {
        public Map<String, Object> faturaOcrTara(byte[] dosyaBytes) {
            log.info("[ALTYAPI TASLAĞI] Fatura OCR tarama servisi tetiklendi.");
            return Map.of(
                "status", "MOCK_SUCCESS",
                "message", "OCR Servis altyapısı hazır. Etkinleştirildiğinde taranacaktır.",
                "tahminiUnvan", "Örnek Tedarikçi A.Ş.",
                "tahminiTutar", 1250.00
            );
        }
    }

    /**
     * 2. Açık Bankacılık (Open Banking) Entegrasyon Altyapı Taslağı
     */
    @Service
    @Slf4j
    public static class OpenBankingStubService {
        public Map<String, Object> bankaHareketleriEslestir(String ibankNo) {
            log.info("[ALTYAPI TASLAĞI] Açık bankacılık hareket eşleme servisi tetiklendi. IBAN: {}", ibankNo);
            return Map.of("status", "ACTIVE_STUB", "eslesenKayitSayisi", 0);
        }
    }

    /**
     * 3. Pazaryeri E-Ticaret Entegratör Altyapı Taslağı
     */
    @Service
    @Slf4j
    public static class PazaryeriStubService {
        public void siparisleriSenkronizeEt(String pazaryeriAdi) {
            log.info("[ALTYAPI TASLAĞI] {} pazaryeri sipariş senkronizasyonu hazır.", pazaryeriAdi);
        }
    }

    /**
     * 4. WhatsApp & SMS Bildirim Motoru Altyapı Taslağı
     */
    @Service
    @Slf4j
    public static class WhatsappSmsStubService {
        public void whatsappMesajGonder(String telefon, String mesaj) {
            log.info("[ALTYAPI TASLAĞI] WhatsApp mesajı kuyruğa alındı -> To: {}", telefon);
        }
    }
}
