package com.raspel.erp.controller;

import com.raspel.erp.dto.SirketDTO;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.service.SirketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SirketController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class SirketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private SirketService sirketService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(SirketDTO.builder().id(1L).ad("Raspel A.Ş.").build());
        when(sirketService.tumunuGetir(any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/sirketler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("Raspel A.Ş."));
    }

    @Test
    void shouldGetAktif() throws Exception {
        var list = List.of(SirketDTO.builder().id(1L).ad("Aktif Şirket").aktif(true).build());
        when(sirketService.aktifOlanlariGetir()).thenReturn(list);

        mockMvc.perform(get("/api/sirketler/aktif"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].aktif").value(true));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = SirketDTO.builder().id(1L).ad("Raspel A.Ş.").vergiNo("1234567890").build();
        when(sirketService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/sirketler/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vergiNo").value("1234567890"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(sirketService.getir(anyLong())).thenThrow(new ResourceNotFoundException("Şirket", 999L));

        mockMvc.perform(get("/api/sirketler/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = SirketDTO.builder().id(1L).ad("Yeni Şirket").build();
        when(sirketService.olustur(any(SirketDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/sirketler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Yeni Şirket"));
    }

    @Test
    void shouldUpdate() throws Exception {
        var dto = SirketDTO.builder().id(1L).ad("Güncel Şirket").build();
        when(sirketService.guncelle(eq(1L), any(SirketDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/sirketler/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Güncel Şirket"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(sirketService).sil(1L);

        mockMvc.perform(delete("/api/sirketler/1"))
                .andExpect(status().isNoContent());
    }
}








