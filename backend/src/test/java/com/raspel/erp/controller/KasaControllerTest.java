package com.raspel.erp.controller;

import com.raspel.erp.dto.finans.KasaDTO;
import com.raspel.erp.dto.finans.KasaHareketDTO;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.service.finans.KasaService;
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
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.controller.finans.KasaController;

@WebMvcTest(KasaController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class KasaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private KasaService kasaService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(KasaDTO.builder().id(1L).ad("Ana Kasa").bakiye(BigDecimal.valueOf(10000)).build());
        when(kasaService.tumKasalarGetir(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/kasalar").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("Ana Kasa"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = KasaDTO.builder().id(1L).ad("Ana Kasa").build();
        when(kasaService.kasaGetir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/kasalar/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Ana Kasa"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(kasaService.kasaGetir(anyLong())).thenThrow(new ResourceNotFoundException("Kasa", 999L));

        mockMvc.perform(get("/api/kasalar/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = KasaDTO.builder().id(1L).ad("Yeni Kasa").build();
        when(kasaService.kasaOlustur(any(KasaDTO.class), anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/kasalar")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Yeni Kasa"));
    }

    @Test
    void shouldUpdate() throws Exception {
        var dto = KasaDTO.builder().id(1L).ad("Güncel Kasa").build();
        when(kasaService.kasaGuncelle(eq(1L), any(KasaDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/kasalar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Güncel Kasa"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(kasaService).kasaSil(1L);

        mockMvc.perform(delete("/api/kasalar/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldGetHareketler() throws Exception {
        var list = List.of(KasaHareketDTO.builder().id(1L).tur("GELIR").tutar(BigDecimal.valueOf(500)).build());
        when(kasaService.kasaHareketleriGetir(1L)).thenReturn(list);

        mockMvc.perform(get("/api/kasalar/1/hareketler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tur").value("GELIR"));
    }

    @Test
    void shouldAddHareket() throws Exception {
        var dto = KasaHareketDTO.builder().id(1L).kasaId(1L).tur("GELIR").tutar(BigDecimal.valueOf(500)).hareketTarihi(LocalDate.now()).build();
        when(kasaService.hareketEkle(any(KasaHareketDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/kasalar/1/hareketler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tur").value("GELIR"));
    }

    @Test
    void shouldDeleteHareket() throws Exception {
        doNothing().when(kasaService).hareketSil(1L);

        mockMvc.perform(delete("/api/kasalar/hareketler/1"))
                .andExpect(status().isNoContent());
    }
}




