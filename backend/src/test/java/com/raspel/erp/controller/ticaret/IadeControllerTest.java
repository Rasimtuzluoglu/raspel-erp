package com.raspel.erp.controller.ticaret;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.ticaret.IadeDTO;
import com.raspel.erp.service.ticaret.IadeService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IadeController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class IadeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private IadeService iadeService;

    private IadeDTO ornek() {
        return IadeDTO.builder().id(1L).faturaId(1L)
                .tarih(LocalDate.now()).tutar(new BigDecimal("240"))
                .durum("TASLAK").build();
    }

    @Test
    void shouldGetAll() throws Exception {
        when(iadeService.tumunuGetir(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornek())));
        mockMvc.perform(get("/api/iadeler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].durum").value("TASLAK"));
    }

    @Test
    void shouldGetById() throws Exception {
        when(iadeService.getir(1L)).thenReturn(ornek());
        mockMvc.perform(get("/api/iadeler/1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldCreate() throws Exception {
        when(iadeService.olustur(any(IadeDTO.class), anyLong())).thenReturn(ornek());
        mockMvc.perform(post("/api/iadeler")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ornek())))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldUpdate() throws Exception {
        when(iadeService.guncelle(eq(1L), any(IadeDTO.class))).thenReturn(ornek());
        mockMvc.perform(put("/api/iadeler/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ornek())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(iadeService).sil(1L);
        mockMvc.perform(delete("/api/iadeler/1"))
                .andExpect(status().isNoContent());
    }
}
