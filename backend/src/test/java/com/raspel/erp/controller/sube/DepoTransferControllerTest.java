package com.raspel.erp.controller.sube;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.sube.DepoTransferDTO;
import com.raspel.erp.service.sube.DepoTransferService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DepoTransferController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class DepoTransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DepoTransferService depoTransferService;

    @Test
    void listele_donar() throws Exception {
        when(depoTransferService.listele(any())).thenReturn(List.of(
                DepoTransferDTO.builder().id(1L).durum("BEKLEMEDE").build()));

        mockMvc.perform(get("/api/depo-transferler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].durum").value("BEKLEMEDE"));
    }

    @Test
    void bekleyenler_donar() throws Exception {
        when(depoTransferService.bekleyenler(any())).thenReturn(List.of(
                DepoTransferDTO.builder().id(1L).durum("BEKLEMEDE").build()));

        mockMvc.perform(get("/api/depo-transferler/bekleyenler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void onayla_donar() throws Exception {
        when(depoTransferService.onayla(1L)).thenReturn(DepoTransferDTO.builder().id(1L).durum("ONAYLANDI").build());

        mockMvc.perform(post("/api/depo-transferler/1/onayla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("ONAYLANDI"));
    }
}
