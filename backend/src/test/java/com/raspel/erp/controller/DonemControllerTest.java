package com.raspel.erp.controller;

import com.raspel.erp.dto.sistem.DonemDTO;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.service.sistem.DonemService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.raspel.erp.controller.sistem.DonemController;
import com.raspel.erp.entity.sistem.Sirket;

@WebMvcTest(DonemController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class DonemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private DonemService donemService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(DonemDTO.builder().id(1L).ad("2024-1 Dönemi").aktif(true).build());
        when(donemService.tumunuGetir(isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/donemler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("2024-1 Dönemi"));
    }

    @Test
    void shouldGetBySirket() throws Exception {
        var list = List.of(DonemDTO.builder().id(1L).ad("2024-1 Dönemi").build());
        when(donemService.sirketeGoreGetir(1L)).thenReturn(list);

        mockMvc.perform(get("/api/donemler/sirket/1").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("2024-1 Dönemi"));
    }

    @Test
    void shouldGetBySirket_throwsWhenForeignCompany() throws Exception {
        mockMvc.perform(get("/api/donemler/sirket/1").requestAttr("sirketId", 2L))
                .andExpect(status().isNotFound());
        verify(donemService, never()).sirketeGoreGetir(anyLong());
    }

    @Test
    void shouldGetAktifBySirket() throws Exception {
        var list = List.of(DonemDTO.builder().id(1L).ad("Aktif Dönem").aktif(true).build());
        when(donemService.aktifDonemler(1L)).thenReturn(list);

        mockMvc.perform(get("/api/donemler/sirket/1/aktif").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].aktif").value(true));
    }

    @Test
    void shouldGetAktifBySirket_throwsWhenForeignCompany() throws Exception {
        mockMvc.perform(get("/api/donemler/sirket/1/aktif").requestAttr("sirketId", 3L))
                .andExpect(status().isNotFound());
        verify(donemService, never()).aktifDonemler(anyLong());
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = DonemDTO.builder().id(1L).ad("2024-1 Dönemi").build();
        when(donemService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/donemler/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("2024-1 Dönemi"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(donemService.getir(anyLong())).thenThrow(new ResourceNotFoundException("Dönem", 999L));

        mockMvc.perform(get("/api/donemler/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = DonemDTO.builder().id(1L).ad("Yeni Dönem").sirketId(1L).baslangic(LocalDate.now()).bitis(LocalDate.now().plusMonths(6)).build();
        when(donemService.olustur(any(DonemDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/donemler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Yeni Dönem"));
    }

    @Test
    void shouldUpdate() throws Exception {
        var dto = DonemDTO.builder().id(1L).ad("Güncel Dönem").build();
        when(donemService.guncelle(eq(1L), any(DonemDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/donemler/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Güncel Dönem"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(donemService).sil(1L);

        mockMvc.perform(delete("/api/donemler/1"))
                .andExpect(status().isNoContent());
    }
}




