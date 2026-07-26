package com.raspel.erp.controller;

import com.raspel.erp.dto.BankaDTO;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.service.BankaService;
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
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankaController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class BankaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private BankaService bankaService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(BankaDTO.builder().id(1L).ad("Ziraat Bankası").bakiye(BigDecimal.valueOf(10000)).build());
        when(bankaService.tumBankalariGetir(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/bankalar").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("Ziraat Bankası"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = BankaDTO.builder().id(1L).ad("Ziraat Bankası").hesapNo("123456").iban("TR123456789").build();
        when(bankaService.bankaGetir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/bankalar/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hesapNo").value("123456"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(bankaService.bankaGetir(anyLong())).thenThrow(new ResourceNotFoundException("Banka", 999L));

        mockMvc.perform(get("/api/bankalar/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = BankaDTO.builder().id(1L).ad("Yeni Banka").hesapNo("654321").build();
        when(bankaService.bankaOlustur(any(BankaDTO.class), anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/bankalar")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Yeni Banka"));
    }

    @Test
    void shouldUpdate() throws Exception {
        var dto = BankaDTO.builder().id(1L).ad("Güncel Banka").hesapNo("111222").build();
        when(bankaService.bankaGuncelle(eq(1L), any(BankaDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/bankalar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Güncel Banka"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(bankaService).bankaSil(1L);

        mockMvc.perform(delete("/api/bankalar/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnErrorWhenDeleteNonExistent() throws Exception {
        doThrow(new ResourceNotFoundException("Banka", 999L)).when(bankaService).bankaSil(999L);

        mockMvc.perform(delete("/api/bankalar/999"))
                .andExpect(status().isNotFound());
    }
}








