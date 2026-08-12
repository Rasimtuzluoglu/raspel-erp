package com.raspel.erp.controller;

import com.raspel.erp.controller.envanter.StokSayimController;
import com.raspel.erp.dto.envanter.StokSayimDTO;
import com.raspel.erp.service.envanter.StokSayimService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StokSayimController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class StokSayimControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private StokSayimService stokSayimService;

    private StokSayimDTO ornek() {
        return StokSayimDTO.builder().id(1L).stokId(1L).stokAdi("Kalem")
                .tarih(LocalDate.now())
                .beklenenMiktar(BigDecimal.TEN)
                .sayilanMiktar(BigDecimal.TEN)
                .durum("TASLAK").sirketId(1L).build();
    }

    @Test
    void shouldGetAll() throws Exception {
        when(stokSayimService.tumunuGetir(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornek())));
        mockMvc.perform(get("/api/stok-sayim").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].durum").value("TASLAK"));
    }

    @Test
    void shouldGetById() throws Exception {
        when(stokSayimService.getir(1L)).thenReturn(ornek());
        mockMvc.perform(get("/api/stok-sayim/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stokAdi").value("Kalem"));
    }

    @Test
    void shouldCreate() throws Exception {
        when(stokSayimService.olustur(any(StokSayimDTO.class), anyLong())).thenReturn(ornek());
        mockMvc.perform(post("/api/stok-sayim")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ornek())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durum").value("TASLAK"));
    }

    @Test
    void shouldUpdate() throws Exception {
        when(stokSayimService.guncelle(eq(1L), any(StokSayimDTO.class))).thenReturn(ornek());
        mockMvc.perform(put("/api/stok-sayim/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ornek())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stokAdi").value("Kalem"));
    }

    @Test
    void shouldUpdateDurum() throws Exception {
        when(stokSayimService.durumGuncelle(eq(1L), eq("TAMAMLANDI"))).thenReturn(ornek());
        mockMvc.perform(put("/api/stok-sayim/1/durum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durum\":\"TAMAMLANDI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("TASLAK"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(stokSayimService).sil(1L);
        mockMvc.perform(delete("/api/stok-sayim/1"))
                .andExpect(status().isNoContent());
    }
}
