package com.raspel.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.controller.finans.PosTerminaliController;
import com.raspel.erp.dto.finans.PosTerminaliDTO;
import com.raspel.erp.service.finans.PosGunSonuService;
import com.raspel.erp.service.finans.PosTerminaliService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PosTerminaliController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class PosTerminaliControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PosTerminaliService posService;
    @MockBean private PosGunSonuService gunSonuService;

    @Test
    void shouldListTerminaller() throws Exception {
        var dto = PosTerminaliDTO.builder().id(1L).ad("Kasa POS").bankaId(2L).komisyonOrani(BigDecimal.valueOf(1.5)).build();
        when(posService.liste(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/pos-terminalleri").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("Kasa POS"));
    }

    @Test
    void shouldCreateTerminal() throws Exception {
        var dto = PosTerminaliDTO.builder().ad("Yeni POS").bankaId(2L).build();
        when(posService.olustur(any(PosTerminaliDTO.class), eq(1L))).thenReturn(dto);

        mockMvc.perform(post("/api/pos-terminalleri")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Yeni POS"));
    }

    @Test
    void shouldRejectTerminalWithoutBanka() throws Exception {
        mockMvc.perform(post("/api/pos-terminalleri")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ad\":\"X\"}")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteTerminal() throws Exception {
        mockMvc.perform(delete("/api/pos-terminalleri/4").requestAttr("sirketId", 1L))
                .andExpect(status().isNoContent());

        verify(posService).sil(4L, 1L);
    }
}
