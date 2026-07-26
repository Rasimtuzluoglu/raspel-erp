package com.raspel.erp.controller;

import com.raspel.erp.dto.FaturaDTO;
import com.raspel.erp.dto.FaturaKalemDTO;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.service.FaturaService;
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
import java.util.Map;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FaturaController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class FaturaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FaturaService faturaService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(FaturaDTO.builder().id(1L).faturaNumarasi("FTR-001").tur("SATIS").build());
        when(faturaService.tumFaturalariGetir(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/faturalar").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].faturaNumarasi").value("FTR-001"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = FaturaDTO.builder().id(1L).faturaNumarasi("FTR-001").tur("SATIS").build();
        when(faturaService.faturaGetir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/faturalar/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faturaNumarasi").value("FTR-001"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(faturaService.faturaGetir(anyLong())).thenThrow(new ResourceNotFoundException("Fatura", 999L));

        mockMvc.perform(get("/api/faturalar/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreate() throws Exception {
        var kalem = FaturaKalemDTO.builder().aciklama("Ürün").adet(2).birimFiyat(BigDecimal.valueOf(100)).build();
        var dto = FaturaDTO.builder().id(1L).faturaNumarasi("FTR-001").tarih(LocalDate.now()).tur("SATIS").kalemler(List.of(kalem)).build();
        when(faturaService.faturaOlustur(any(FaturaDTO.class), anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/faturalar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.faturaNumarasi").value("FTR-001"));
    }

    @Test
    void shouldUpdate() throws Exception {
        var kalem = FaturaKalemDTO.builder().aciklama("Guncel Kalem").adet(1).birimFiyat(BigDecimal.valueOf(200)).build();
        var dto = FaturaDTO.builder().id(1L).tur("SATIS").tarih(LocalDate.now()).kalemler(List.of(kalem)).build();
        when(faturaService.faturaGuncelle(eq(1L), any(FaturaDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/faturalar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldUpdateDurum() throws Exception {
        var dto = FaturaDTO.builder().id(1L).durum("KESILDI").build();
        when(faturaService.faturaDurumGuncelle(eq(1L), anyString())).thenReturn(dto);

        mockMvc.perform(put("/api/faturalar/1/durum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("durum", "KESILDI"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("KESILDI"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(faturaService).faturaSil(1L);

        mockMvc.perform(delete("/api/faturalar/1"))
                .andExpect(status().isNoContent());
    }
}








