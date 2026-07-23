package com.raspel.erp.controller;

import com.raspel.erp.dto.StokDTO;
import com.raspel.erp.dto.StokHareketDTO;
import com.raspel.erp.service.StokService;
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
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StokController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class StokControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private StokService stokService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(StokDTO.builder().id(1L).ad("Kalem").stokKodu("KLM001").fiyat(BigDecimal.valueOf(10)).build());
        when(stokService.tumunuGetir(anyLong(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(list));

        mockMvc.perform(get("/api/stoklar").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("Kalem"));
    }

    @Test
    void shouldSearch() throws Exception {
        var list = List.of(StokDTO.builder().id(1L).ad("Kalem").build());
        when(stokService.ara("Kalem")).thenReturn(list);

        mockMvc.perform(get("/api/stoklar/ara").param("q", "Kalem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("Kalem"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = StokDTO.builder().id(1L).ad("Kalem").stokKodu("KLM001").build();
        when(stokService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/stoklar/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stokKodu").value("KLM001"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(stokService.getir(anyLong())).thenThrow(new RuntimeException("Stok bulunamadı: 999"));

        mockMvc.perform(get("/api/stoklar/999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = StokDTO.builder().id(1L).ad("Yeni Ürün").fiyat(BigDecimal.valueOf(25)).build();
        when(stokService.olustur(any(StokDTO.class), anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/stoklar")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Yeni Ürün"));
    }

    @Test
    void shouldUpdate() throws Exception {
        var dto = StokDTO.builder().id(1L).ad("Güncel Ürün").fiyat(BigDecimal.valueOf(30)).build();
        when(stokService.guncelle(eq(1L), any(StokDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/stoklar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Güncel Ürün"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(stokService).sil(1L);

        mockMvc.perform(delete("/api/stoklar/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldGetHareketler() throws Exception {
        var list = List.of(StokHareketDTO.builder().id(1L).tur("GIRIS").miktar(BigDecimal.valueOf(10)).build());
        when(stokService.hareketler(1L)).thenReturn(list);

        mockMvc.perform(get("/api/stoklar/1/hareketler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tur").value("GIRIS"));
    }

    @Test
    void shouldGetAllHareketler() throws Exception {
        var list = List.of(StokHareketDTO.builder().id(1L).tur("CIKIS").miktar(BigDecimal.valueOf(5)).build());
        when(stokService.tumHareketler()).thenReturn(list);

        mockMvc.perform(get("/api/stoklar/hareketler/tum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tur").value("CIKIS"));
    }

    @Test
    void shouldAddHareket() throws Exception {
        var dto = StokHareketDTO.builder().id(1L).stokId(1L).tur("GIRIS").miktar(BigDecimal.valueOf(10)).hareketTarihi(LocalDate.now()).build();
        when(stokService.hareketEkle(any(StokHareketDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/stoklar/1/hareketler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tur").value("GIRIS"));
    }

    @Test
    void shouldDeleteHareket() throws Exception {
        doNothing().when(stokService).hareketSil(1L);

        mockMvc.perform(delete("/api/stoklar/hareketler/1"))
                .andExpect(status().isNoContent());
    }
}








