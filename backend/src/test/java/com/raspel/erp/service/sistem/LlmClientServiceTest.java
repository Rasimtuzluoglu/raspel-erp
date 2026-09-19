package com.raspel.erp.service.sistem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmClientServiceTest {

    private final LlmClientService service = new LlmClientService();

    @Test
    void sendQuery_providerNullIseHataFirlatir() {
        assertThrows(IllegalArgumentException.class,
                () -> service.sendQuery(null, "gpt-4", "key", "sys", "user"));
    }

    @Test
    void sendQuery_desteklenmeyenProviderHataFirlatir() {
        // sendQuery, desteklenmeyen saglayiciyi kontrollu RuntimeException'a sarar.
        RuntimeException hata = assertThrows(RuntimeException.class,
                () -> service.sendQuery("BILINMEYEN", "model", "key", "sys", "user"));
        assertTrue(hata.getMessage().contains("BILINMEYEN") || hata.getCause() instanceof IllegalArgumentException);
    }
}
