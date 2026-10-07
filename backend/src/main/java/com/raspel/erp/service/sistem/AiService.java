package com.raspel.erp.service.sistem;

import java.util.function.Consumer;

/**
 * Faz 4.1: AI sağlayıcı bağımsız port. Uygulama katmanı (dashboard özeti, ajanda
 * doğal dil, sohbet özeti) bu arayüze bağlanır; somut sağlayıcı (OpenAI/Gemini/
 * Claude) {@code LlmAiService} içinde çözülür.
 */
public interface AiService {

    /** Şirket için AI yapılandırılmış ve aktif mi? */
    boolean yapilandirilmis(Long sirketId);

    /** Yapılandırılmış modelle senkron yanıt üretir. */
    String sorgula(Long sirketId, String sistemPrompt, String kullaniciPrompt);

    /** SSE/stream için token bazlı yanıt üretir. */
    void akisSorgula(Long sirketId, String sistemPrompt, String kullaniciPrompt, Consumer<String> onToken);
}
