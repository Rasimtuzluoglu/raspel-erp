package com.raspel.erp.controller.ik;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.ik.VardiyaDTO;
import com.raspel.erp.service.ik.VardiyaService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VardiyaController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class VardiyaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private VardiyaService vardiyaService;

    private VardiyaDTO ornek() {
        return VardiyaDTO.builder().id(1L).personelId(1L).personelAdi("Ahmet Yilmaz")
                .tarih(LocalDate.of(2026, 8, 15))
                .baslangic(LocalTime.of(8, 0)).bitis(LocalTime.of(17, 0))
                .tur("SABAH").sirketId(1L).build();
    }

    @Test
    void shouldGetAll() throws Exception {
        when(vardiyaService.tumunuGetir(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornek())));
        mockMvc.perform(get("/api/vardiyalar").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tur").value("SABAH"));
    }

    @Test
    void shouldGetById() throws Exception {
        when(vardiyaService.getir(1L)).thenReturn(ornek());
        mockMvc.perform(get("/api/vardiyalar/1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetByPersonel() throws Exception {
        when(vardiyaService.personelVardiyalari(1L)).thenReturn(List.of(ornek()));
        mockMvc.perform(get("/api/vardiyalar/personel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].personelId").value(1L));
    }

    @Test
    void shouldCreate() throws Exception {
        when(vardiyaService.olustur(any(VardiyaDTO.class), anyLong())).thenReturn(ornek());
        mockMvc.perform(post("/api/vardiyalar")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ornek())))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldGetFiltreli() throws Exception {
        var list = List.of(VardiyaDTO.builder().id(1L).personelId(2L).tur("GECE").build());
        when(vardiyaService.filtreli(eq(1L), eq(2L), any(), any(), eq("GECE"), eq("ahmet"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/vardiyalar/filtreli")
                        .requestAttr("sirketId", 1L)
                        .param("personelId", "2")
                        .param("baslangic", "2026-10-01")
                        .param("bitis", "2026-10-31")
                        .param("tur", "GECE")
                        .param("q", "ahmet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tur").value("GECE"));

        verify(vardiyaService).filtreli(eq(1L), eq(2L),
                any(java.time.LocalDate.class), any(java.time.LocalDate.class), eq("GECE"), eq("ahmet"), any(Pageable.class));
    }

    @Test
    void shouldGetOzet() throws Exception {
        when(vardiyaService.ozet(1L)).thenReturn(com.raspel.erp.dto.ik.VardiyaOzetDTO.builder()
                .toplam(30).bugun(4).buHafta(11).personelSayisi(7).build());

        mockMvc.perform(get("/api/vardiyalar/ozet").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplam").value(30))
                .andExpect(jsonPath("$.bugun").value(4))
                .andExpect(jsonPath("$.buHafta").value(11))
                .andExpect(jsonPath("$.personelSayisi").value(7));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(vardiyaService).sil(1L);
        mockMvc.perform(delete("/api/vardiyalar/1"))
                .andExpect(status().isNoContent());
    }
}
