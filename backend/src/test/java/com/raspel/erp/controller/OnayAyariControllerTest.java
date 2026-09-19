package com.raspel.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.controller.sistem.OnayAyariController;
import com.raspel.erp.dto.sistem.OnayAyariDTO;
import com.raspel.erp.service.sistem.OnayAyariService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OnayAyariController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class OnayAyariControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private OnayAyariService onayAyariService;

    @Test
    void shouldListAyarlar() throws Exception {
        var dto = OnayAyariDTO.builder().id(1L).modul("FATURA").esikTutar(BigDecimal.valueOf(5000)).build();
        when(onayAyariService.listele(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/onay-ayarlari").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modul").value("FATURA"));
    }

    @Test
    void shouldSaveAyari() throws Exception {
        var dto = OnayAyariDTO.builder().modul("FATURA").esikTutar(BigDecimal.valueOf(1000)).otomatikOnay(false).build();
        when(onayAyariService.kaydet(eq(1L), any(OnayAyariDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/onay-ayarlari")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modul").value("FATURA"));
    }
}
