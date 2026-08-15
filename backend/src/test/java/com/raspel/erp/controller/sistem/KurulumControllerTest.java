package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.sistem.KurulumDTO;
import com.raspel.erp.dto.sistem.LoginResponse;
import com.raspel.erp.service.sistem.KurulumService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KurulumController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class KurulumControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private KurulumService kurulumService;

    @Test
    void shouldGetDurum() throws Exception {
        when(kurulumService.durum()).thenReturn(Map.of("kurulumGerekli", true));
        mockMvc.perform(get("/api/kurulum/durum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kurulumGerekli").value(true));
    }

    @Test
    void shouldBaslat() throws Exception {
        when(kurulumService.kurulumYap(any(KurulumDTO.class)))
                .thenReturn(LoginResponse.builder().id(1L).username("yonetici").girisToken("tok").build());
        mockMvc.perform(post("/api/kurulum/baslat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ad\":\"Yeni Firma\",\"vergiNo\":\"1234567890\",\"adminUsername\":\"yonetici\",\"adminPassword\":\"Sifre1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("yonetici"));
    }
}
