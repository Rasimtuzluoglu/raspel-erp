package com.raspel.erp.controller;

import com.raspel.erp.dto.RaporDTO;
import com.raspel.erp.service.RaporService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@WebMvcTest(RaporController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class RaporControllerTest {

    @Autowired
    private MockMvc mockMvc;

    
    @MockBean
    private RaporService raporService;

    @Test
    void shouldGetCariEkstre() throws Exception {
        var dto = RaporDTO.CariEkstreDTO.builder().cariAd("ABC Müşteri").donemBasBakiye(BigDecimal.ZERO).donemSonBakiye(BigDecimal.valueOf(5000)).build();
        when(raporService.cariEkstreGetir(anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(dto);

        mockMvc.perform(get("/api/raporlar/cari-ekstre")
                        .param("cariHesapId", "1")
                        .param("baslangic", "2024-01-01")
                        .param("bitis", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cariAd").value("ABC Müşteri"));
    }

    @Test
    void shouldGetGelirGider() throws Exception {
        var dto = RaporDTO.GelirGiderOzetDTO.builder().toplamGelir(BigDecimal.valueOf(50000)).toplamGider(BigDecimal.valueOf(30000)).netKarZarar(BigDecimal.valueOf(20000)).build();
        when(raporService.gelirGiderOzeti(any(LocalDate.class), any(LocalDate.class))).thenReturn(dto);

        mockMvc.perform(get("/api/raporlar/gelir-gider")
                        .param("baslangic", "2024-01-01")
                        .param("bitis", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplamGelir").value(50000));
    }

    @Test
    void shouldGetKdvRaporu() throws Exception {
        var dto = RaporDTO.KdvRaporDTO.builder().toplamKdvCikis(BigDecimal.valueOf(1000)).toplamKdvGiris(BigDecimal.valueOf(500)).kdvFarki(BigDecimal.valueOf(500)).build();
        when(raporService.kdvRaporu(any(LocalDate.class), any(LocalDate.class))).thenReturn(dto);

        mockMvc.perform(get("/api/raporlar/kdv")
                        .param("baslangic", "2024-01-01")
                        .param("bitis", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kdvFarki").value(500));
    }

    @Test
    void shouldGetYaslandirma() throws Exception {
        var list = List.of(RaporDTO.YaslandirmaDTO.builder().cariAd("Müşteri").bakiye(BigDecimal.valueOf(10000)).gun(45).aralik("31-60 Gün").build());
        when(raporService.yaslandirmaRaporu()).thenReturn(list);

        mockMvc.perform(get("/api/raporlar/yaslandirma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cariAd").value("Müşteri"));
    }
}








