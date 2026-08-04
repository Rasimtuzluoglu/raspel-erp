package com.raspel.erp.controller;

import com.raspel.erp.dto.ticaret.SatinalmaSiparisDTO;
import com.raspel.erp.service.ticaret.SatinalmaSiparisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.raspel.erp.exception.ResourceNotFoundException;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.raspel.erp.controller.ticaret.SatinalmaSiparisController;

@WebMvcTest(SatinalmaSiparisController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class SatinalmaSiparisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private SatinalmaSiparisService satinalmaSiparisService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(SatinalmaSiparisDTO.builder().id(1L).siparisNo("SAT-001").durum("TASLAK").build());
        when(satinalmaSiparisService.tumunuGetir(isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/satinalma-siparisler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].siparisNo").value("SAT-001"));
    }

    @Test
    void shouldGetAllBySirket() throws Exception {
        var list = List.of(SatinalmaSiparisDTO.builder().id(1L).siparisNo("SAT-001").build());
        when(satinalmaSiparisService.tumunuGetir(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/satinalma-siparisler").param("sirketId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].siparisNo").value("SAT-001"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = SatinalmaSiparisDTO.builder().id(1L).siparisNo("SAT-001").build();
        when(satinalmaSiparisService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/satinalma-siparisler/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siparisNo").value("SAT-001"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(satinalmaSiparisService.getir(anyLong())).thenThrow(new ResourceNotFoundException("Sipariş bulunamadı: 999"));

        mockMvc.perform(get("/api/satinalma-siparisler/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = SatinalmaSiparisDTO.builder().id(1L).siparisNo("SAT-001").tarih(LocalDate.now()).build();
        when(satinalmaSiparisService.olustur(any(SatinalmaSiparisDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/satinalma-siparisler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.siparisNo").value("SAT-001"));
    }

    @Test
    void shouldUpdateDurum() throws Exception {
        var dto = SatinalmaSiparisDTO.builder().id(1L).durum("ONAYLANDI").build();
        when(satinalmaSiparisService.durumGuncelle(eq(1L), anyString())).thenReturn(dto);

        mockMvc.perform(put("/api/satinalma-siparisler/1/durum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("durum", "ONAYLANDI"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("ONAYLANDI"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(satinalmaSiparisService).sil(1L);

        mockMvc.perform(delete("/api/satinalma-siparisler/1"))
                .andExpect(status().isNoContent());
    }
}





