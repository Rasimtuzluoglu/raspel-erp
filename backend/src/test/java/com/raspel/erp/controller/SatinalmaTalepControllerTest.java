package com.raspel.erp.controller;

import com.raspel.erp.dto.SatinalmaTalepDTO;
import com.raspel.erp.service.SatinalmaTalepService;
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

@WebMvcTest(SatinalmaTalepController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class SatinalmaTalepControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private SatinalmaTalepService satinalmaTalepService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(SatinalmaTalepDTO.builder().id(1L).talepNo("TALEP-001").durum("TASLAK").build());
        when(satinalmaTalepService.tumunuGetir(null)).thenReturn(list);

        mockMvc.perform(get("/api/satinalma-talepler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].talepNo").value("TALEP-001"));
    }

    @Test
    void shouldGetAllBySirket() throws Exception {
        var list = List.of(SatinalmaTalepDTO.builder().id(1L).talepNo("TALEP-001").build());
        when(satinalmaTalepService.tumunuGetir(1L)).thenReturn(list);

        mockMvc.perform(get("/api/satinalma-talepler").param("sirketId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].talepNo").value("TALEP-001"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = SatinalmaTalepDTO.builder().id(1L).talepNo("TALEP-001").build();
        when(satinalmaTalepService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/satinalma-talepler/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.talepNo").value("TALEP-001"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(satinalmaTalepService.getir(anyLong())).thenThrow(new RuntimeException("Talep bulunamadı: 999"));

        mockMvc.perform(get("/api/satinalma-talepler/999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = SatinalmaTalepDTO.builder().id(1L).talepNo("TALEP-001").tarih(LocalDate.now()).talepEden("Ahmet").build();
        when(satinalmaTalepService.olustur(any(SatinalmaTalepDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/satinalma-talepler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.talepNo").value("TALEP-001"));
    }

    @Test
    void shouldUpdateDurum() throws Exception {
        var dto = SatinalmaTalepDTO.builder().id(1L).durum("ONAYLANDI").build();
        when(satinalmaTalepService.durumGuncelle(eq(1L), anyString())).thenReturn(dto);

        mockMvc.perform(put("/api/satinalma-talepler/1/durum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("durum", "ONAYLANDI"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("ONAYLANDI"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(satinalmaTalepService).sil(1L);

        mockMvc.perform(delete("/api/satinalma-talepler/1"))
                .andExpect(status().isNoContent());
    }
}








