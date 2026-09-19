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
        when(takipService.zincir(1L)).thenReturn(List.of(SiparisTakipDTO.builder().build()));

        mockMvc.perform(get("/api/siparis-takip").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnEmptyZincir() throws Exception {
        when(takipService.zincir(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/siparis-takip").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }
}
