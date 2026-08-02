package com.raspel.erp.controller.finans;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.finans.ButceDTO;
import com.raspel.erp.service.finans.ButceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ButceController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class ButceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ButceService butceService;

    private ButceDTO ornek() {
        return ButceDTO.builder().id(1L).ad("2026 Bütçe").yil(2026).ay(1)
                .tutar(new BigDecimal("50000")).tur("GIDER").build();
    }

    @Test
    void shouldGetAll() throws Exception {
        when(butceService.tumunuGetir(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornek())));
        mockMvc.perform(get("/api/butceler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("2026 Bütçe"));
    }

    @Test
    void shouldGetById() throws Exception {
        when(butceService.getir(1L)).thenReturn(ornek());
        mockMvc.perform(get("/api/butceler/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yil").value(2026));
    }

    @Test
    void shouldCreate() throws Exception {
        when(butceService.olustur(any(ButceDTO.class), anyLong())).thenReturn(ornek());
        mockMvc.perform(post("/api/butceler")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ornek())))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldUpdate() throws Exception {
        when(butceService.guncelle(eq(1L), any(ButceDTO.class))).thenReturn(ornek());
        mockMvc.perform(put("/api/butceler/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ornek())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(butceService).sil(1L);
        mockMvc.perform(delete("/api/butceler/1"))
                .andExpect(status().isNoContent());
    }
}
