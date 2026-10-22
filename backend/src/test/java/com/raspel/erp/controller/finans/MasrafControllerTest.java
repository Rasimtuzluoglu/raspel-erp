package com.raspel.erp.controller.finans;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.finans.MasrafDTO;
import com.raspel.erp.service.finans.MasrafService;
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

@WebMvcTest(MasrafController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class MasrafControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private MasrafService masrafService;

    private MasrafDTO ornek() {
        return MasrafDTO.builder().id(1L).tarih(LocalDate.now())
                .tutar(new BigDecimal("750")).aciklama("Kırtasiye").kategori("Ofis").build();
    }

    @Test
    void shouldGetAll() throws Exception {
        when(masrafService.tumunuGetir(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornek())));
        mockMvc.perform(get("/api/masraflar").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].aciklama").value("Kırtasiye"));
    }

    @Test
    void shouldGetById() throws Exception {
        when(masrafService.getir(1L)).thenReturn(ornek());
        mockMvc.perform(get("/api/masraflar/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kategori").value("Ofis"));
    }

    @Test
    void shouldCreate() throws Exception {
        when(masrafService.olustur(any(MasrafDTO.class), anyLong())).thenReturn(ornek());
        mockMvc.perform(post("/api/masraflar")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ornek())))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldGetFiltreli() throws Exception {
        var list = List.of(MasrafDTO.builder().id(1L).kategori("OFIS").tutar(new BigDecimal("750")).build());
        when(masrafService.filtreli(eq(1L), eq("OFIS"), any(), any(), eq("kirtasiye"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/masraflar/filtreli")
                        .requestAttr("sirketId", 1L)
                        .param("kategori", "OFIS")
                        .param("baslangic", "2026-01-01")
                        .param("bitis", "2026-12-31")
                        .param("q", "kirtasiye"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].kategori").value("OFIS"));

        verify(masrafService).filtreli(eq(1L), eq("OFIS"),
                any(java.time.LocalDate.class), any(java.time.LocalDate.class), eq("kirtasiye"), any(Pageable.class));
    }

    @Test
    void shouldGetOzet() throws Exception {
        when(masrafService.ozet(1L)).thenReturn(com.raspel.erp.dto.finans.MasrafOzetDTO.builder()
                .buAyToplam(new BigDecimal("1000")).buYilToplam(new BigDecimal("5000"))
                .buYilKdv(new BigDecimal("900")).toplamKayit(12L).build());

        mockMvc.perform(get("/api/masraflar/ozet").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buAyToplam").value(1000))
                .andExpect(jsonPath("$.buYilToplam").value(5000))
                .andExpect(jsonPath("$.buYilKdv").value(900))
                .andExpect(jsonPath("$.toplamKayit").value(12));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(masrafService).sil(1L);
        mockMvc.perform(delete("/api/masraflar/1"))
                .andExpect(status().isNoContent());
    }
}
