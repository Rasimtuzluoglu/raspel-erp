package com.raspel.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.controller.sistem.YoneticiKokpitController;
import com.raspel.erp.dto.sistem.SirketHedefDTO;
import com.raspel.erp.dto.sistem.YoneticiKokpitDTO;
import com.raspel.erp.service.sistem.YoneticiKokpitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(YoneticiKokpitController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class YoneticiKokpitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private YoneticiKokpitService kokpitService;

    @Test
    void shouldGetKokpitVerileri() throws Exception {
        var dto = YoneticiKokpitDTO.builder()
                .yil(2026)
                .ay(8)
                .gerceklesenCiro(BigDecimal.valueOf(75000))
                .hedefCiro(BigDecimal.valueOf(100000))
                .ciroIlerlemeYuzdesi(75.0)
                .build();

        when(kokpitService.getKokpitVerileri(eq(1L), eq(2026), eq(8))).thenReturn(dto);

        mockMvc.perform(get("/api/yonetici-kokpit")
                        .param("yil", "2026")
                        .param("ay", "8")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gerceklesenCiro").value(75000))
                .andExpect(jsonPath("$.ciroIlerlemeYuzdesi").value(75.0));
    }

    @Test
    void shouldSaveHedef() throws Exception {
        var dto = SirketHedefDTO.builder()
                .yil(2026)
                .ay(8)
                .hedefCiro(BigDecimal.valueOf(250000))
                .build();

        when(kokpitService.hedefKaydet(any(SirketHedefDTO.class), eq(1L))).thenReturn(dto);

        mockMvc.perform(post("/api/yonetici-kokpit/hedef")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hedefCiro").value(250000));
    }
}
