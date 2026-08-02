package com.raspel.erp.controller;

import com.raspel.erp.dto.HareketDTO;
import com.raspel.erp.service.HareketService;
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
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HareketController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class HareketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private HareketService hareketService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(HareketDTO.builder().id(1L).tur("TAHSILAT").tutar(BigDecimal.valueOf(1000)).build());
        when(hareketService.tumHareketleriGetir(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/hareketler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tur").value("TAHSILAT"));
    }

    @Test
    void shouldGetWithFilters() throws Exception {
        var list = List.of(HareketDTO.builder().id(1L).tur("TAHSILAT").build());
        when(hareketService.hareketleriFiltrele(any(), any(), any())).thenReturn(list);

        mockMvc.perform(get("/api/hareketler")
                        .param("cariHesapId", "1")
                        .param("baslangic", "2024-01-01")
                        .param("bitis", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tur").value("TAHSILAT"));
    }

    @Test
    void shouldGetByCariHesap() throws Exception {
        var list = List.of(HareketDTO.builder().id(1L).cariHesapId(1L).build());
        when(hareketService.cariHesapHareketleriGetir(1L)).thenReturn(list);

        mockMvc.perform(get("/api/hareketler/cari/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cariHesapId").value(1L));
    }

    @Test
    void shouldGetSon() throws Exception {
        var list = List.of(HareketDTO.builder().id(1L).build());
        when(hareketService.sonHareketleriGetir(5)).thenReturn(list);

        mockMvc.perform(get("/api/hareketler/son/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void shouldExportCsv() throws Exception {
        var list = List.of(HareketDTO.builder().id(1L).tur("TAHSILAT").tutar(BigDecimal.valueOf(1000)).build());
        when(hareketService.tumHareketleriGetir(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/hareketler/export/csv").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"hareketler.csv\""));
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = HareketDTO.builder().id(1L).cariHesapId(1L).tur("TAHSILAT").tutar(BigDecimal.valueOf(1000)).build();
        when(hareketService.hareketOlustur(any(HareketDTO.class), anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/hareketler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tur").value("TAHSILAT"));
    }

    @Test
    void shouldUpdate() throws Exception {
        var dto = HareketDTO.builder().id(1L).cariHesapId(1L).tur("ODEME").tutar(BigDecimal.valueOf(500)).build();
        when(hareketService.hareketGuncelle(eq(1L), any(HareketDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/hareketler/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tur").value("ODEME"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(hareketService).hareketSil(1L);

        mockMvc.perform(delete("/api/hareketler/1"))
                .andExpect(status().isNoContent());
    }
}








