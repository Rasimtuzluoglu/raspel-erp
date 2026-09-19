package com.raspel.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.controller.envanter.StokDuzeltmeController;
import com.raspel.erp.dto.envanter.StokDuzeltmeDTO;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.service.envanter.StokDuzeltmeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StokDuzeltmeController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class StokDuzeltmeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private StokDuzeltmeService duzeltmeService;

    @Test
    void shouldGetGecmis() throws Exception {
        var dto = StokDuzeltmeDTO.builder().id(1L).stokId(5L).yeniMiktar(BigDecimal.TEN).neden("sayım").build();
        when(duzeltmeService.gecmis(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/stok-duzeltme").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].neden").value("sayım"));
    }

    @Test
    void shouldDuzelt() throws Exception {
        var dto = StokDuzeltmeDTO.builder().stokId(5L).yeniMiktar(BigDecimal.TEN).neden("sayım").build();
        when(duzeltmeService.duzelt(any(StokDuzeltmeDTO.class), eq(1L), eq(2L))).thenReturn(dto);

        mockMvc.perform(post("/api/stok-duzeltme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 2L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stokId").value(5));
    }

    @Test
    void shouldRejectInvalidDuzeltme() throws Exception {
        // stokId ve neden zorunlu -> dogrulama 400
        mockMvc.perform(post("/api/stok-duzeltme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"yeniMiktar\": 5}")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 2L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestOnBusinessError() throws Exception {
        var dto = StokDuzeltmeDTO.builder().stokId(5L).yeniMiktar(BigDecimal.TEN).neden("x").build();
        when(duzeltmeService.duzelt(any(StokDuzeltmeDTO.class), anyLong(), anyLong()))
                .thenThrow(new BusinessException("stok bulunamadı"));

        mockMvc.perform(post("/api/stok-duzeltme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 2L))
                .andExpect(status().isBadRequest());
    }
}
