package com.raspel.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.controller.ticaret.SiparisTakipController;
import com.raspel.erp.dto.ticaret.SiparisTakipDTO;
import com.raspel.erp.service.ticaret.SiparisTakipService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SiparisTakipController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class SiparisTakipControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SiparisTakipService takipService;

    @Test
    void shouldGetZincir() throws Exception {
        when(takipService.filtreliZincir(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(SiparisTakipDTO.builder().build())));

        mockMvc.perform(get("/api/siparis-takip").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnEmptyZincir() throws Exception {
        when(takipService.filtreliZincir(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        mockMvc.perform(get("/api/siparis-takip").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetOzet() throws Exception {
        when(takipService.ozet(1L)).thenReturn(com.raspel.erp.dto.ticaret.SiparisTakipOzetDTO.builder()
                .toplam(10).bekleyen(6).uretimde(3).sevkte(2).teslimEdildi(3).geciken(2).iptal(1).build());

        mockMvc.perform(get("/api/siparis-takip/ozet").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.toplam").value(10))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.geciken").value(2));
    }
}
