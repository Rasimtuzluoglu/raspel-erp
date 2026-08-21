package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AiConfigDTO;
import com.raspel.erp.entity.sistem.AiConfig;
import com.raspel.erp.repository.sistem.AiConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiConfigService {

    private final AiConfigRepository aiConfigRepository;
    private final LlmClientService llmClientService;

    @Value("${ai.encryption.key:DefaultDevKey12345678901234567890}")
    private String encryptionKey;

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Transactional(readOnly = true)
    public AiConfigDTO getConfig(Long sirketId) {
        Optional<AiConfig> configOpt = aiConfigRepository.findBySirketId(sirketId);
        
        if (configOpt.isEmpty()) {
            return AiConfigDTO.builder()
                    .durum("YAPILANDIRILMADI")
                    .build();
        }

        AiConfig config = configOpt.get();
        String decryptedKey = decrypt(config.getEncryptedApiKey());
        String durum = Boolean.TRUE.equals(config.getAktif()) ? "AKTIF" : "PASIF";

        return AiConfigDTO.builder()
                .provider(config.getProvider())
                .apiKey(maskKey(decryptedKey))
                .model(config.getModel())
                .aktif(config.getAktif())
                .durum(durum)
                .build();
    }

    @Transactional
    public AiConfigDTO saveConfig(AiConfigDTO dto, Long sirketId) {
        AiConfig config = aiConfigRepository.findBySirketId(sirketId)
                .orElse(AiConfig.builder().sirketId(sirketId).build());

        config.setProvider(dto.getProvider());
        config.setModel(dto.getModel());
        config.setAktif(dto.getAktif() != null ? dto.getAktif() : true);

        // Update API key if provided and not masked
        if (dto.getApiKey() != null && !dto.getApiKey().contains("****")) {
            config.setEncryptedApiKey(encrypt(dto.getApiKey()));
        }

        aiConfigRepository.save(config);
        return getConfig(sirketId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> testConnection(Long sirketId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Optional<AiConfig> configOpt = aiConfigRepository.findBySirketId(sirketId);
            if (configOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", "Yapilandirma bulunamadi");
                return result;
            }

            AiConfig config = configOpt.get();
            String decryptedKey = decrypt(config.getEncryptedApiKey());
            
            String testPrompt = "Please reply exactly with 'OK'.";
            String response = llmClientService.sendQuery(config.getProvider(), config.getModel(), decryptedKey, "You are a test assistant.", testPrompt);
            
            result.put("success", true);
            result.put("message", "Baglanti basarili. Yanit: " + response);
        } catch (Exception e) {
            log.error("AI Connection test failed for sirket: {}", sirketId, e);
            result.put("success", false);
            result.put("message", "Baglanti basarisiz: " + e.getMessage());
        }
        return result;
    }

    @Transactional
    public void deleteConfig(Long sirketId) {
        aiConfigRepository.deleteBySirketId(sirketId);
    }

    @Transactional(readOnly = true)
    public String getDecryptedKey(Long sirketId) {
        return aiConfigRepository.findBySirketId(sirketId)
                .map(config -> decrypt(config.getEncryptedApiKey()))
                .orElse(null);
    }

    private String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length > 32) {
                byte[] temp = new byte[32];
                System.arraycopy(keyBytes, 0, temp, 0, 32);
                keyBytes = temp;
            }
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
            byte[] encryptedText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] ivAndEncryptedText = new byte[iv.length + encryptedText.length];
            System.arraycopy(iv, 0, ivAndEncryptedText, 0, iv.length);
            System.arraycopy(encryptedText, 0, ivAndEncryptedText, iv.length, encryptedText.length);

            return Base64.getEncoder().encodeToString(ivAndEncryptedText);
        } catch (Exception e) {
            log.error("Error while encrypting API key", e);
            throw new RuntimeException("API Key sifreleme hatasi", e);
        }
    }

    private String decrypt(String encryptedTextBase64) {
        try {
            byte[] ivAndEncryptedText = Base64.getDecoder().decode(encryptedTextBase64);

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(ivAndEncryptedText, 0, iv, 0, iv.length);

            byte[] encryptedText = new byte[ivAndEncryptedText.length - GCM_IV_LENGTH];
            System.arraycopy(ivAndEncryptedText, GCM_IV_LENGTH, encryptedText, 0, encryptedText.length);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length > 32) {
                byte[] temp = new byte[32];
                System.arraycopy(keyBytes, 0, temp, 0, 32);
                keyBytes = temp;
            }
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);
            byte[] decryptedText = cipher.doFinal(encryptedText);

            return new String(decryptedText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error while decrypting API key", e);
            throw new RuntimeException("API Key desifreleme hatasi", e);
        }
    }

    private String maskKey(String key) {
        if (key == null || key.length() <= 7) {
            return "****";
        }
        return key.substring(0, 3) + "****" + key.substring(key.length() - 4);
    }
}
