package com.raspel.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.controller.ticaret.TeklifController;
import com.raspel.erp.dto.ticaret.SiparisDTO;
import com.raspel.erp.dto.ticaret.TeklifDTO;
import com.raspel.erp.service.ticaret.TeklifService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeklifController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class TeklifControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeklifService teklifService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(TeklifDTO.builder().id(1L).teklifNo("TKL-2026-0001").durum("TASLAK").build());
        when(teklifService.tumunuGetir(any(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/teklifler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].teklifNo").value("TKL-2026-0001"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = TeklifDTO.builder().id(1L).teklifNo("TKL-2026-0001").build();
        when(teklifService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/teklifler/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teklifNo").value("TKL-2026-0001"));
    }

    @Test
    void shouldCreateTeklif() throws Exception {
        var dto = TeklifDTO.builder()
                .teklifNo("TKL-2026-0001")
                .cariHesapId(1L)
                .tarih(LocalDate.now())
                .genelToplam(BigDecimal.valueOf(1000))
                .build();
        when(teklifService.olustur(any(TeklifDTO.class), eq(1L))).thenReturn(dto);

        mockMvc.perform(post("/api/teklifler")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teklifNo").value("TKL-2026-0001"));
    }

    @Test
    void shouldConvertToSiparis() throws Exception {
        var sipDto = SiparisDTO.builder().id(10L).siparisNo("SIP-001").build();
        when(teklifService.sipariseDonustur(1L)).thenReturn(sipDto);

        mockMvc.perform(post("/api/teklifler/1/siparise-donustur"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siparisNo").value("SIP-001"));
    }
}
