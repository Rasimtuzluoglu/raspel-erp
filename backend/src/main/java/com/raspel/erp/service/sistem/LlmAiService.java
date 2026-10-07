package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AiConfigDTO;
import com.raspel.erp.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * Faz 4.1: {@link AiService} portunun LLM (OpenAI/Gemini/Claude) implementasyonu.
 * Şirket bazlı sağlayıcı/model/anahtar {@link AiConfigService} üzerinden çözülür
 * ve çağrı {@link LlmClientService}'e delege edilir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlmAiService implements AiService {

    private final AiConfigService aiConfigService;
    private final LlmClientService llmClientService;

    @Override
    public boolean yapilandirilmis(Long sirketId) {
        if (sirketId == null) return false;
        try {
            AiConfigDTO c = aiConfigService.getConfig(sirketId);
            return c != null && c.getProvider() != null
                    && !"YAPILANDIRILMADI".equalsIgnoreCase(c.getDurum());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String sorgula(Long sirketId, String sistemPrompt, String kullaniciPrompt) {
        AiConfigDTO c = yapilandirmaAl(sirketId);
        String key = aiConfigService.getDecryptedKey(sirketId);
        return llmClientService.sendQuery(c.getProvider(), c.getModel(), key, sistemPrompt, kullaniciPrompt);
    }

    @Override
    public void akisSorgula(Long sirketId, String sistemPrompt, String kullaniciPrompt, Consumer<String> onToken) {
        AiConfigDTO c = yapilandirmaAl(sirketId);
        String key = aiConfigService.getDecryptedKey(sirketId);
        llmClientService.streamQuery(c.getProvider(), c.getModel(), key, sistemPrompt, kullaniciPrompt, onToken);
    }

    private AiConfigDTO yapilandirmaAl(Long sirketId) {
        if (sirketId == null) {
            throw new BusinessException("AI için şirket bağlamı bulunamadı");
        }
        AiConfigDTO c = aiConfigService.getConfig(sirketId);
        if (c == null || c.getProvider() == null
                || "YAPILANDIRILMADI".equalsIgnoreCase(c.getDurum())) {
            throw new BusinessException("AI yapılandırılmamış. Ayarlar > AI Yapılandırması bölümünden ekleyin.");
        }
        return c;
    }
}
