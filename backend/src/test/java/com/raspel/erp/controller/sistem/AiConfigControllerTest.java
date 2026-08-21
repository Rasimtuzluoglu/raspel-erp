package com.raspel.erp.controller.sistem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.sistem.AiConfigDTO;
import com.raspel.erp.service.sistem.AiConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiConfigController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class AiConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiConfigService aiConfigService;

    @Test
    void shouldGetConfig() throws Exception {
        AiConfigDTO dto = AiConfigDTO.builder()
                .provider("OPENAI")
                .durum("AKTIF")
                .build();

        when(aiConfigService.getConfig(any())).thenReturn(dto);

        mockMvc.perform(get("/api/ai-config")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("OPENAI"))
                .andExpect(jsonPath("$.durum").value("AKTIF"));
    }

    @Test
    void shouldSaveConfig() throws Exception {
        AiConfigDTO input = AiConfigDTO.builder()
                .provider("OPENAI")
                .build();

        AiConfigDTO output = AiConfigDTO.builder()
                .provider("OPENAI")
                .durum("AKTIF")
                .build();

        when(aiConfigService.saveConfig(any(AiConfigDTO.class), any())).thenReturn(output);

        mockMvc.perform(post("/api/ai-config")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("OPENAI"))
                .andExpect(jsonPath("$.durum").value("AKTIF"));
    }

    @Test
    void shouldTestConnection() throws Exception {
        when(aiConfigService.testConnection(any())).thenReturn(Map.of("status", "SUCCESS", "message", "OK"));

        mockMvc.perform(post("/api/ai-config/test")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void shouldDeleteConfig() throws Exception {
        mockMvc.perform(delete("/api/ai-config")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isNoContent());

        verify(aiConfigService).deleteConfig(any());
    }
}
