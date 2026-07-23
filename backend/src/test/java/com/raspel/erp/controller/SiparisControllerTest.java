package com.raspel.erp.controller;

import com.raspel.erp.dto.SiparisDTO;
import com.raspel.erp.service.SiparisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SiparisController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class SiparisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private SiparisService siparisService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(SiparisDTO.builder().id(1L).siparisNo("SPR-001").durum("TEKLIF").build());
        when(siparisService.tumunuGetir(null)).thenReturn(list);

        mockMvc.perform(get("/api/siparisler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].siparisNo").value("SPR-001"));
    }

    @Test
    void shouldGetAllBySirket() throws Exception {
        var list = List.of(SiparisDTO.builder().id(1L).siparisNo("SPR-001").build());
        when(siparisService.tumunuGetir(1L)).thenReturn(list);

        mockMvc.perform(get("/api/siparisler").param("sirketId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].siparisNo").value("SPR-001"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = SiparisDTO.builder().id(1L).siparisNo("SPR-001").build();
        when(siparisService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/siparisler/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siparisNo").value("SPR-001"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(siparisService.getir(anyLong())).thenThrow(new RuntimeException("Sipariş bulunamadı: 999"));

        mockMvc.perform(get("/api/siparisler/999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = SiparisDTO.builder().id(1L).siparisNo("SPR-001").tarih(LocalDate.now()).build();
        when(siparisService.olustur(any(SiparisDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/siparisler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.siparisNo").value("SPR-001"));
    }

    @Test
    void shouldUpdateDurum() throws Exception {
        var dto = SiparisDTO.builder().id(1L).durum("ONAYLANDI").build();
        when(siparisService.durumGuncelle(eq(1L), anyString())).thenReturn(dto);

        mockMvc.perform(put("/api/siparisler/1/durum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("durum", "ONAYLANDI"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("ONAYLANDI"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(siparisService).sil(1L);

        mockMvc.perform(delete("/api/siparisler/1"))
                .andExpect(status().isNoContent());
    }
}








