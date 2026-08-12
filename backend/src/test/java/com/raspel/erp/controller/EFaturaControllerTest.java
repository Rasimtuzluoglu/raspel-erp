package com.raspel.erp.controller;

import com.raspel.erp.controller.ticaret.EFaturaController;
import com.raspel.erp.dto.ticaret.EFaturaDTO;
import com.raspel.erp.service.ticaret.EFaturaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EFaturaController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class EFaturaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private EFaturaService eFaturaService;

    private EFaturaDTO ornek() {
        return EFaturaDTO.builder().id(1L).faturaId(1L)
                .ettn("ettn-1").faturaNo("FTR-001")
                .senaryo("TEMELFATURA").tip("SATIS")
                .gibDurumKodu(1000).sirketId(1L)
                .odenecekTutar(new BigDecimal("120.00")).build();
    }

    @Test
    void shouldGetAll() throws Exception {
        when(eFaturaService.eFaturalariGetir(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornek())));
        mockMvc.perform(get("/api/e-fatura").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ettn").value("ettn-1"));
    }

    @Test
    void shouldGetById() throws Exception {
        when(eFaturaService.eFaturaGetir(1L)).thenReturn(ornek());
        mockMvc.perform(get("/api/e-fatura/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faturaNo").value("FTR-001"));
    }

    @Test
    void shouldCreate() throws Exception {
        when(eFaturaService.eFaturaOlustur(eq(1L), eq("TEMELFATURA"), eq("SATIS"), anyLong()))
                .thenReturn(ornek());
        mockMvc.perform(post("/api/e-fatura/olustur/1")
                        .requestAttr("sirketId", 1L)
                        .param("senaryo", "TEMELFATURA")
                        .param("tip", "SATIS"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ettn").value("ettn-1"));
    }

    @Test
    void shouldSendToGib() throws Exception {
        when(eFaturaService.gibGonder(1L)).thenReturn(ornek());
        mockMvc.perform(post("/api/e-fatura/1/gib-gonder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gibDurumKodu").value(1000));
    }

    @Test
    void shouldDownloadXml() throws Exception {
        when(eFaturaService.xmlIndir(1L)).thenReturn("<Invoice></Invoice>");
        mockMvc.perform(get("/api/e-fatura/1/xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(content().string(Matchers.containsString("Invoice")));
    }
}
