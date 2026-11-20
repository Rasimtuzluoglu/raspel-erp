package com.raspel.erp.controller.sistem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.dto.sistem.FrontendHataDTO;
import com.raspel.erp.service.sistem.TelemetriService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.health.redis.enabled=false",
        "management.health.rabbit.enabled=false",
        "management.health.db.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TelemetriControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private TelemetriService telemetriService;

    private String govde() throws Exception {
        return objectMapper.writeValueAsString(FrontendHataDTO.builder()
                .hataId("hata-1")
                .tur("vue")
                .mesaj("Test hatasi")
                .stack("at x (y.js:1)\nat z (w.js:2)")
                .rota("/cari-hesaplar")
                .surum("1.36.3")
                .tarayici("Chrome")
                .build());
    }

    @Test
    void oturumluKullanici202Alir() throws Exception {
        when(telemetriService.kabulEt(anyLong())).thenReturn(true);

        mockMvc.perform(post("/api/telemetri/hata")
                        .with(user("u").roles("USER"))
                        .requestAttr("kullaniciId", 9L)
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(govde()))
                .andExpect(status().isAccepted());

        verify(telemetriService).kaydet(any(FrontendHataDTO.class), eq(9L), eq(1L));
    }

    @Test
    void oturumsuzIstekReddedilir() throws Exception {
        mockMvc.perform(post("/api/telemetri/hata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(govde()))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(telemetriService);
    }

    @Test
    void hizAsiminda429Doner() throws Exception {
        when(telemetriService.kabulEt(anyLong())).thenReturn(false);

        mockMvc.perform(post("/api/telemetri/hata")
                        .with(user("u").roles("USER"))
                        .requestAttr("kullaniciId", 9L)
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(govde()))
                .andExpect(status().isTooManyRequests());

        verify(telemetriService, never()).kaydet(any(), anyLong(), anyLong());
    }

    @Test
    void mesajsizGovde400Doner() throws Exception {
        mockMvc.perform(post("/api/telemetri/hata")
                        .with(user("u").roles("USER"))
                        .requestAttr("kullaniciId", 9L)
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tur\":\"vue\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(telemetriService);
    }
}
