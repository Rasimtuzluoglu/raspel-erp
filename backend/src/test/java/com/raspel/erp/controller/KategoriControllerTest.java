package com.raspel.erp.controller;

import com.raspel.erp.dto.KategoriDTO;
import com.raspel.erp.service.KategoriService;
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
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KategoriController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class KategoriControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private KategoriService kategoriService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(KategoriDTO.builder().id(1L).ad("Gelir").tur("GELIR").build());
        when(kategoriService.tumunuGetir(anyLong())).thenReturn(list);

        mockMvc.perform(get("/api/kategoriler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("Gelir"));
    }

    @Test
    void shouldGetByTur() throws Exception {
        var list = List.of(KategoriDTO.builder().id(1L).ad("Maaş").tur("GIDER").build());
        when(kategoriService.turuGetir("GIDER")).thenReturn(list);

        mockMvc.perform(get("/api/kategoriler/tur/GIDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("Maaş"));
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = KategoriDTO.builder().id(1L).ad("Yeni Kategori").tur("GELIR").build();
        when(kategoriService.olustur(any(KategoriDTO.class), anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/kategoriler")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Yeni Kategori"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(kategoriService).sil(1L);

        mockMvc.perform(delete("/api/kategoriler/1"))
                .andExpect(status().isNoContent());
    }
}








