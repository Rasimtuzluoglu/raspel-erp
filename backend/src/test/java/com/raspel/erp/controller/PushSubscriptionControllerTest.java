package com.raspel.erp.controller;

import com.raspel.erp.controller.sistem.PushSubscriptionController;
import com.raspel.erp.dto.sistem.PushAbonelikDTO;
import com.raspel.erp.service.sistem.WebPushService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PushSubscriptionController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class PushSubscriptionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private WebPushService webPushService;

    @Test
    void shouldVapidPublicKey() throws Exception {
        when(webPushService.aktif()).thenReturn(true);
        when(webPushService.publicKey()).thenReturn("BPublic");

        mockMvc.perform(get("/api/push/vapid-public-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value("BPublic"))
                .andExpect(jsonPath("$.aktif").value(true));
    }

    @Test
    void shouldAbone() throws Exception {
        PushAbonelikDTO dto = PushAbonelikDTO.builder()
                .endpoint("https://push/x")
                .keys(PushAbonelikDTO.Keys.builder().p256dh("p").auth("a").build())
                .build();
        when(webPushService.aboneOl(any(PushAbonelikDTO.class), any(), any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/push/abone")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoint").value("https://push/x"));
    }

    @Test
    void shouldAboneSil() throws Exception {
        doNothing().when(webPushService).aboneSil("https://push/x");
        mockMvc.perform(delete("/api/push/abone").param("endpoint", "https://push/x"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldTestPush() throws Exception {
        when(webPushService.gonder(eq(1L), anyString(), anyString(), anyString())).thenReturn(2);
        when(webPushService.aktif()).thenReturn(true);

        mockMvc.perform(post("/api/push/test").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gonderilen").value(2));
    }
}
