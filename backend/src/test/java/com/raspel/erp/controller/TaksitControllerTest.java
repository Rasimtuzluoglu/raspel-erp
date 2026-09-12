package com.raspel.erp.controller;

import com.raspel.erp.controller.finans.TaksitController;
import com.raspel.erp.dto.finans.TaksitDTO;
import com.raspel.erp.dto.finans.TaksitOdeDTO;
import com.raspel.erp.dto.finans.TaksitPlanDTO;
import com.raspel.erp.service.finans.TaksitService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaksitController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class TaksitControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private TaksitService taksitService;

    private TaksitDTO ornek() {
        return TaksitDTO.builder()
                .id(1L).sirketId(1L).cariId(2L).cariAd("Test Cari")
                .planNo("TKS-1").taksitNo(1).taksitSayisi(3)
                .vadeTarihi(LocalDate.now().plusDays(10))
                .tutar(new BigDecimal("100.00"))
                .odemeDurumu("BEKLEMEDE")
                .build();
    }

    @Test
    void shouldListele() throws Exception {
        when(taksitService.listele(anyLong(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornek())));
        mockMvc.perform(get("/api/taksitler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].odemeDurumu").value("BEKLEMEDE"));
    }

    @Test
    void shouldYaklasan() throws Exception {
        when(taksitService.yaklasan(1L, 30)).thenReturn(List.of(ornek()));
        mockMvc.perform(get("/api/taksitler/yaklasan").param("gun", "30").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cariAd").value("Test Cari"));
    }

    @Test
    void shouldTakvim() throws Exception {
        when(taksitService.takvim(1L, 2026, 5)).thenReturn(List.of(ornek()));
        mockMvc.perform(get("/api/taksitler/takvim")
                        .param("yil", "2026").param("ay", "5").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planNo").value("TKS-1"));
    }

    @Test
    void shouldOzet() throws Exception {
        when(taksitService.ozet(1L)).thenReturn(Map.of(
                "bekleyenToplam", new BigDecimal("600"),
                "gecikmisToplam", new BigDecimal("300"),
                "buAyToplam", new BigDecimal("200"),
                "bekleyenAdet", 3,
                "gecikmisAdet", 1));
        mockMvc.perform(get("/api/taksitler/ozet").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bekleyenAdet").value(3));
    }

    @Test
    void shouldPlanOlustur() throws Exception {
        when(taksitService.planOlustur(any(TaksitPlanDTO.class), eq(1L))).thenReturn(List.of(ornek()));
        TaksitPlanDTO dto = TaksitPlanDTO.builder()
                .cariId(2L).toplamTutar(new BigDecimal("300")).taksitSayisi(3).build();
        mockMvc.perform(post("/api/taksitler/plan")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].taksitNo").value(1));
    }

    @Test
    void shouldOde() throws Exception {
        TaksitDTO odendi = ornek();
        odendi.setOdemeDurumu("ODENDI");
        when(taksitService.ode(eq(1L), any(TaksitOdeDTO.class), eq(1L))).thenReturn(odendi);
        mockMvc.perform(post("/api/taksitler/1/ode")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hareketId\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.odemeDurumu").value("ODENDI"));
    }

    @Test
    void shouldSil() throws Exception {
        doNothing().when(taksitService).sil(1L, 1L);
        mockMvc.perform(delete("/api/taksitler/1").requestAttr("sirketId", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldPlanSil() throws Exception {
        doNothing().when(taksitService).planSil("TKS-1", 1L);
        mockMvc.perform(delete("/api/taksitler/plan/TKS-1").requestAttr("sirketId", 1L))
                .andExpect(status().isNoContent());
    }
}
