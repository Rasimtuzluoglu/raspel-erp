package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AiConfigDTO;
import com.raspel.erp.entity.sistem.AiConfig;
import com.raspel.erp.repository.sistem.AiConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiConfigServiceTest {

    @Mock
    private AiConfigRepository aiConfigRepository;

    @Mock
    private LlmClientService llmClientService;

    @InjectMocks
    private AiConfigService aiConfigService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiConfigService, "encryptionKey", "DefaultDevKey12345678901234567890");
    }

    @Test
    void getConfig_whenNoConfig_returnsYapilandirilmadi() {
        when(aiConfigRepository.findBySirketId(1L)).thenReturn(Optional.empty());

        AiConfigDTO dto = aiConfigService.getConfig(1L);

        assertEquals("YAPILANDIRILMADI", dto.getDurum());
    }

    @Test
    void saveConfig_encryptsAndSaves() throws Exception {
        AiConfigDTO input = AiConfigDTO.builder()
                .provider("OPENAI")
                .apiKey("sk-1234567890123456")
                .model("gpt-4o")
                .aktif(true)
                .build();

        // First findBySirketId returns empty, then after save returns saved entity
        AiConfig savedEntity = new AiConfig();
        when(aiConfigRepository.findBySirketId(1L)).thenAnswer(inv -> {
            if (savedEntity.getEncryptedApiKey() != null) {
                return Optional.of(savedEntity);
            }
            return Optional.empty();
        });

        when(aiConfigRepository.save(any(AiConfig.class))).thenAnswer(inv -> {
            AiConfig arg = inv.getArgument(0);
            savedEntity.setId(10L);
            savedEntity.setSirketId(arg.getSirketId());
            savedEntity.setProvider(arg.getProvider());
            savedEntity.setEncryptedApiKey(arg.getEncryptedApiKey());
            savedEntity.setModel(arg.getModel());
            savedEntity.setAktif(arg.getAktif());
            return savedEntity;
        });

        AiConfigDTO result = aiConfigService.saveConfig(input, 1L);

        assertNotNull(result);
        assertEquals("OPENAI", result.getProvider());
        assertTrue(result.getApiKey().contains("****"));
        assertEquals("AKTIF", result.getDurum());
        verify(aiConfigRepository, times(1)).save(any(AiConfig.class));
    }

    @Test
    void getConfig_returnsMaskedKey() throws Exception {
        String plainKey = "sk-1234567890123456";
        java.lang.reflect.Method encryptMethod = AiConfigService.class.getDeclaredMethod("encrypt", String.class);
        encryptMethod.setAccessible(true);
        String encryptedKey = (String) encryptMethod.invoke(aiConfigService, plainKey);

        AiConfig config = AiConfig.builder()
                .sirketId(1L)
                .provider("OPENAI")
                .encryptedApiKey(encryptedKey)
                .model("gpt-4o")
                .aktif(true)
                .build();

        when(aiConfigRepository.findBySirketId(1L)).thenReturn(Optional.of(config));

        AiConfigDTO dto = aiConfigService.getConfig(1L);

        assertEquals("OPENAI", dto.getProvider());
        assertEquals("sk-****3456", dto.getApiKey());
        assertEquals("AKTIF", dto.getDurum());
    }

    @Test
    void deleteConfig_removesConfig() {
        aiConfigService.deleteConfig(1L);
        verify(aiConfigRepository, times(1)).deleteBySirketId(1L);
    }
}
