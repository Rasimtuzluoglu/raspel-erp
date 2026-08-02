package com.raspel.erp.controller;

import com.raspel.erp.dto.CariHesapDTO;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.service.CariHesapService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CariHesapController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class CariHesapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private CariHesapService cariHesapService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(CariHesapDTO.builder().id(1L).ad("ABC Müşteri").bakiye(BigDecimal.valueOf(5000)).build());
        when(cariHesapService.tumCariHesaplariGetir(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/cari-hesaplar").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("ABC Müşteri"));
    }

    @Test
    void shouldSearch() throws Exception {
        var list = List.of(CariHesapDTO.builder().id(1L).ad("ABC Müşteri").build());
        when(cariHesapService.cariHesapAra(eq("ABC"), anyLong())).thenReturn(list);

        mockMvc.perform(get("/api/cari-hesaplar/search").param("q", "ABC").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("ABC Müşteri"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = CariHesapDTO.builder().id(1L).ad("ABC Müşteri").telefon("5551234567").build();
        when(cariHesapService.cariHesapGetir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/cari-hesaplar/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefon").value("5551234567"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(cariHesapService.cariHesapGetir(anyLong())).thenThrow(new ResourceNotFoundException("Cari Hesap", 999L));

        mockMvc.perform(get("/api/cari-hesaplar/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = CariHesapDTO.builder().id(1L).ad("Yeni Müşteri").vergiNumarasi("1234567890").build();
        when(cariHesapService.cariHesapOlustur(any(CariHesapDTO.class), anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/cari-hesaplar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Yeni Müşteri"));
    }

    @Test
    void shouldUpdate() throws Exception {
        var dto = CariHesapDTO.builder().id(1L).ad("Güncel Müşteri").build();
        when(cariHesapService.cariHesapGuncelle(eq(1L), any(CariHesapDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/cari-hesaplar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Güncel Müşteri"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(cariHesapService).cariHesapSil(1L);

        mockMvc.perform(delete("/api/cari-hesaplar/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldExportCsv() throws Exception {
        var list = List.of(CariHesapDTO.builder().id(1L).ad("Test").build());
        when(cariHesapService.tumCariHesaplariGetir(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/cari-hesaplar/export/csv").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"cari-hesaplar.csv\""));
    }
}








