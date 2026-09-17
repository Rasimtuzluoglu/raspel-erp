package com.raspel.erp.controller;

import com.raspel.erp.controller.sistem.AdresDefteriController;
import com.raspel.erp.dto.sistem.AdresDefteriDTO;
import com.raspel.erp.service.sistem.AdresDefteriService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdresDefteriController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class AdresDefteriControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdresDefteriService adresDefteriService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(AdresDefteriDTO.builder().id(1L).ad("Elektrikçi Ali").tur("Elektrikçi").build());
        when(adresDefteriService.tumunuGetir(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/adres-defteri").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("Elektrikçi Ali"));
    }

    @Test
    void shouldFilterWhenQueryGiven() throws Exception {
        var list = List.of(AdresDefteriDTO.builder().id(1L).ad("Ali").build());
        when(adresDefteriService.filtrele(eq(1L), eq("ali"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/adres-defteri").param("q", "ali").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("Ali"));
    }

    @Test
    void shouldGetById() throws Exception {
        when(adresDefteriService.getir(1L))
                .thenReturn(AdresDefteriDTO.builder().id(1L).ad("Ali").build());

        mockMvc.perform(get("/api/adres-defteri/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Ali"));
    }

    @Test
    void shouldReturnTurler() throws Exception {
        when(adresDefteriService.turListesi(1L)).thenReturn(List.of("Elektrikçi", "Tesisatçı"));

        mockMvc.perform(get("/api/adres-defteri/turler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Elektrikçi"));
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = AdresDefteriDTO.builder().id(1L).ad("Yeni Kişi").tur("Elektrikçi").build();
        when(adresDefteriService.olustur(any(AdresDefteriDTO.class), anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/adres-defteri")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Yeni Kişi"));
    }

    @Test
    void shouldUpdate() throws Exception {
        var dto = AdresDefteriDTO.builder().id(1L).ad("Güncel").build();
        when(adresDefteriService.guncelle(eq(1L), any(AdresDefteriDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/adres-defteri/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Güncel"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(adresDefteriService).sil(1L);

        mockMvc.perform(delete("/api/adres-defteri/1"))
                .andExpect(status().isNoContent());
    }
}
