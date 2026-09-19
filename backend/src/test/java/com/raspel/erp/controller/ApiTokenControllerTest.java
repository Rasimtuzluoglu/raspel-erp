package com.raspel.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.controller.sistem.ApiTokenController;
import com.raspel.erp.dto.sistem.ApiTokenDTO;
import com.raspel.erp.service.sistem.ApiTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiTokenController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class ApiTokenControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ApiTokenService apiTokenService;

    @Test
    void shouldListTokens() throws Exception {
        when(apiTokenService.listele(3L)).thenReturn(List.of(ApiTokenDTO.builder().id(1L).ad("Mobil").build()));

        mockMvc.perform(get("/api/api-tokenlar").requestAttr("kullaniciId", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("Mobil"));
    }

    @Test
    void shouldCreateToken() throws Exception {
        when(apiTokenService.olustur(eq(3L), anyString())).thenReturn(ApiTokenDTO.builder().id(2L).ad("Entegrasyon").build());

        mockMvc.perform(post("/api/api-tokenlar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ad\":\"Entegrasyon\"}")
                        .requestAttr("kullaniciId", 3L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Entegrasyon"));
    }

    @Test
    void shouldDeleteToken() throws Exception {
        mockMvc.perform(delete("/api/api-tokenlar/9").requestAttr("kullaniciId", 3L))
                .andExpect(status().isNoContent());

        verify(apiTokenService).sil(9L, 3L);
    }
}
